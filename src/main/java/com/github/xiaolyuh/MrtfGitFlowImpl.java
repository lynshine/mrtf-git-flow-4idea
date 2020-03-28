package com.github.xiaolyuh;

import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.github.xiaolyuh.utils.OkHttpClientUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitUtil;
import git4idea.commands.*;
import git4idea.merge.GitMergeCommittingConflictResolver;
import git4idea.merge.GitMerger;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.util.GitFileUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Objects;

import static git4idea.GitUtil.updateAndRefreshVfs;

/**
 * @author yuhao.wang3
 * @since 2020/3/23 9:53
 */
public class MrtfGitFlowImpl implements MrtfGitFlow {
    private Git git = Git.getInstance();

    @Override
    public void addConfigToGit(GitRepository repository, boolean reInit) {
        if (reInit) {
            return;
        }
        try {
            String filePath = repository.getProject().getBasePath() + File.separator + Constants.CONFIG_FILE_NAME;
            FilePath path = VcsUtil.getFilePath(filePath);
            GitFileUtils.addPaths(repository.getProject(), repository.getRoot(), Lists.newArrayList(path));
        } catch (VcsException e) {

        }
    }

    @Override
    public GitCommandResult newNewBranchBaseRemoteMaster(@NotNull GitRepository repository, @Nullable String master, @NotNull String newBranchName, @Nullable GitLineHandlerListener... listeners) {
        //git fetch origin 远程分支名x:本地分支名x
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.FETCH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        // 远程分支名x:本地分支名x
        h.addParameters(master + ":" + newBranchName);
        h.addParameters("-f");

        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }

        git.runCommand(h);
        // 切换分支
        git.checkout(repository, newBranchName, null, true, false);
        // 推送分支
        return push(repository, newBranchName, listeners);
    }

    @Override
    public GitCommandResult renameBranch(@NotNull GitRepository repository,
                                         @Nullable String oldBranch,
                                         @NotNull String newBranchName,
                                         @Nullable GitLineHandlerListener listener) {

        return git.renameBranch(repository, oldBranch, newBranchName, listener);
    }

    @Override
    public GitCommandResult deleteBranch(@NotNull GitRepository repository,
                                         @Nullable String master,
                                         @Nullable String branchName,
                                         @Nullable GitLineHandlerListener listener) {

        git.checkout(repository, master, null, true, false);
        deleteRemoteBranch(repository, branchName, listener);
        // 删除本地分支
        return git.branchDelete(repository, branchName, true, listener);
    }

    @Override
    public String getCurrentBranch(@NotNull Project project) {
        GitRepository repository = GitBranchUtil.getCurrentRepository(project);
        return repository.getCurrentBranch().getName();
    }

    @Override
    public String getRemoteLastCommit(@NotNull GitRepository repository, @Nullable String branch, @Nullable GitLineHandlerListener... listeners) {
        //git show origin/master -s --format=Author:%ae-Date:%ad-Message:%s --date=format:%Y-%m-%d_%H:%M:%S
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.SHOW);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin/" + branch);
        h.addParameters("-s");
        h.addParameters("--format=Author:%ae-Date:%ad-Message:%s");
        h.addParameters("--date=format:%Y-%m-%d_%H:%M:%S");

        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }

        GitCommandResult result = git.runCommand(h);
        String msg = result.getOutputAsJoinedString();
        msg = msg.replaceFirst("Author:", "\r\n  操作人: ");
        msg = msg.replaceFirst("-Date:", ";\r\n  时间: ");
        msg = msg.replaceFirst("-Message:", ";\r\n  Message: ");
        return msg;
    }

    @Override
    public GitCommandResult mergeBranchAndPush(GitRepository repository, String currentBranch, String targetBranch,
                                               TagOptions tagOptions, GitLineHandlerListener errorListener) {
        // 判断目标分支是否存在
        String master = ConfigUtil.getConfig(repository.getProject()).get().getMasterBranch();
        if (!GitBranchUtil.getLocalBranches(repository.getProject()).contains(targetBranch)) {
            GitCommandResult result = newNewBranchBaseRemoteMaster(repository, master, targetBranch, errorListener);
            if (!result.success()) {
                return result;
            }
        }

        // 切换到目标分支
        git.checkout(repository, targetBranch, null, true, false);
        // pull最新代码
        GitCommandResult result = pull(repository, targetBranch, errorListener);
        if (!result.success()) {
            return result;
        }

        // 合并代码
        GitSimpleEventDetector mergeConflict = new GitSimpleEventDetector(GitSimpleEventDetector.Event.MERGE_CONFLICT);
        result = git.merge(repository, currentBranch, Lists.newArrayList(), errorListener, mergeConflict);

        boolean allConflictsResolved = true;
        if (mergeConflict.hasHappened()) {
            // 发生冲突
            updateAndRefreshVfs(repository);
            // 解决冲突
            allConflictsResolved = new MyMergeConflictResolver(repository, currentBranch, targetBranch).merge();
        }

        if (!result.success() && !allConflictsResolved) {
            return result;
        }

        // 打tag
        if (Objects.nonNull(tagOptions)) {
            result = createNewTag(repository, tagOptions.getTagName(), tagOptions.getMessage(), errorListener);
            if (!result.success()) {
                return result;
            }
        }

        // push代码
        result = push(repository, targetBranch, errorListener);
        if (!result.success()) {
            return result;
        }

        // 切换到当前分支
        return git.checkout(repository, currentBranch, null, true, false);
    }

    @Override
    public boolean lock(GitRepository repository, String currentBranch, GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(currentBranch + ":" + Constants.LOCK_BRANCH_NAME);
        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        GitCommandResult result = git.runCommand(h);

        if (result.success() && isNewBranch(result)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean unlock(GitRepository repository) {
        GitCommandResult result = deleteRemoteBranch(repository, Constants.LOCK_BRANCH_NAME);
        if (result.success()) {
            NotifyUtil.notifySuccess(repository.getProject(), "Success", "发布分支已解除锁定，可以再次点击[开始发布]");
        } else {
            NotifyUtil.notifyError(repository.getProject(), "Error", String.format("发布分支解除锁定失败: %s", result.getErrorOutputAsJoinedString()));
        }
        return result.success();
    }

    @Override
    public boolean isLock(Project project) {

        return GitBranchUtil.getRemoteBranches(project).contains(Constants.LOCK_BRANCH_NAME);
    }

    @Override
    public void thirdPartyNotify(GitRepository repository) {
        try {
            String dingtalkToken = ConfigUtil.getConfig(repository.getProject()).get().getDingtalkToken();
            if (StringUtils.isNotBlank(dingtalkToken)) {
                String url = String.format("https://oapi.dingtalk.com/robot/send?access_token=%s", dingtalkToken);
                String msg = getRemoteLastCommit(repository, ConfigUtil.getConfig(repository.getProject()).get().getReleaseBranch());

                msg = String.format("%s 服务发布分支已被锁定，最后一次操作：%s ;\r\n如需强行发布，请先点[发布失败]解除锁定，再点[开始发布]。", repository.getProject().getName(), msg);
                OkHttpClientUtil.postApplicationJson(url, new DingtalkMessage(msg), "钉钉通知接口", String.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    private GitCommandResult createNewTag(@NotNull GitRepository repository, @Nullable String tagName,
                                          @Nullable String message, @Nullable GitLineHandlerListener... listeners) {
        final GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.TAG);
        h.setSilent(false);
        h.addParameters("-a");
        h.addParameters("-f");
        h.addParameters("-m");
        h.addParameters(message);
        h.addParameters(ConfigUtil.getConfig(repository.getProject()).get().getTagPrefix() + tagName);

        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        return git.runCommand(h);
    }

    private GitCommandResult pull(GitRepository repository, @Nullable String branchName, @Nullable GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PULL);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(branchName + ":" + branchName);
        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        return git.runCommand(h);
    }

    private GitCommandResult push(GitRepository repository, String branchName, GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(branchName + ":" + branchName);
        h.addParameters("--tag");
        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        return git.runCommand(h);
    }

    /**
     * 删除远程分支 git push origin --delete dev
     *
     * @param repository repository
     * @param branchName branchName
     * @param listeners  listeners
     * @return GitCommandResult
     */
    private GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @Nullable String branchName, @Nullable GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters("--delete");
        h.addParameters(branchName);

        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }

        return git.runCommand(h);
    }

    private GitRemote getDefaultRemote(@NotNull GitRepository repository) {
        Collection<GitRemote> remotes = repository.getRemotes();
        if (CollectionUtils.isEmpty(remotes)) {
            return null;
        }
        return remotes.iterator().next();
    }

    private boolean isNewBranch(GitCommandResult result) {
        return result.getOutputAsJoinedString().contains("new branch") || result.getErrorOutputAsJoinedString().contains("new branch");
    }

    private class MyMergeConflictResolver extends GitMergeCommittingConflictResolver {
        String currentBranch;
        String targetBranch;

        MyMergeConflictResolver(GitRepository repository, String currentBranch, String targetBranch) {
            super(repository.getProject(), git, new GitMerger(repository.getProject()),
                    GitUtil.getRootsFromRepositories(Lists.newArrayList(repository)), new Params(repository.getProject()), true);
            this.currentBranch = currentBranch;
            this.targetBranch = targetBranch;
        }

        @Override
        protected void notifyUnresolvedRemain() {
            notifyWarning("合并代码冲突", String.format("%s 分支合并到 %s分支发生代码冲突", currentBranch, targetBranch));
        }
    }
}
