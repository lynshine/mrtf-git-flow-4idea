package com.github.xiaolyuh;

import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitUtil;
import git4idea.commands.*;
import git4idea.history.wholeTree.GitCreateNewTag;
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

import static git4idea.GitUtil.HEAD;
import static git4idea.GitUtil.updateAndRefreshVfs;

/**
 * @author yuhao.wang3
 * @since 2020/3/23 9:53
 */
public class MrtfGitFlowImpl implements MrtfGitFlow {

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

        Git.getInstance().runCommand(h);
        // 切换分支
        Git.getInstance().checkout(repository, newBranchName, null, true, false);
        // 推送分支
        return push(repository, newBranchName, listeners);
    }

    @Override
    public GitCommandResult renameBranch(@NotNull GitRepository repository,
                                         @Nullable String oldBranch,
                                         @NotNull String newBranchName,
                                         @Nullable GitLineHandlerListener listener) {

        return Git.getInstance().renameBranch(repository, oldBranch, newBranchName, listener);
    }

    @Override
    public GitCommandResult deleteBranch(@NotNull GitRepository repository,
                                         @Nullable String master,
                                         @Nullable String branchName,
                                         @Nullable GitLineHandlerListener listener) {

        Git.getInstance().checkout(repository, master, null, true, false);
        deleteRemoteBranch(repository, branchName, listener);
        // 删除本地分支
        return Git.getInstance().branchDelete(repository, branchName, true, listener);
    }

    @Override
    public String getCurrentBranch(@NotNull Project project) {
        GitRepository repository = GitBranchUtil.getGitRepository(project);
        return repository.getCurrentBranch().getName();
    }

    @Override
    public GitCommandResult getRemoteLastCommit(@NotNull GitRepository repository, @Nullable String branch, @Nullable GitLineHandlerListener... listeners) {
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

        return Git.getInstance().runCommand(h);
    }

    @Override
    public GitCommandResult mergeBranchAndPush(GitRepository repository, String currentBranch, String targetBranch, String tagName, GitLineHandlerListener errorListener) {
        // 判断目标分支是否存在
        String master = ConfigUtil.getConfig(repository.getProject()).get().getMasterBranch();
        if (!GitBranchUtil.getLocalBranches(repository.getProject()).contains(targetBranch)) {
            GitCommandResult result = newNewBranchBaseRemoteMaster(repository, master, targetBranch, errorListener);
            if (!result.success()) {
                return result;
            }
        }

        // 切换到目标分支
        Git.getInstance().checkout(repository, targetBranch, null, true, false);
        // pull最新代码
        GitCommandResult result = pull(repository, targetBranch, errorListener);
        if (!result.success()) {
            return result;
        }

        // 合并代码
        GitSimpleEventDetector mergeConflict = new GitSimpleEventDetector(GitSimpleEventDetector.Event.MERGE_CONFLICT);
        result = Git.getInstance().merge(repository, currentBranch, Lists.newArrayList(), errorListener, mergeConflict);

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
        if (StringUtils.isNotBlank(tagName)) {
            result =  Git.getInstance().createNewTag(repository, tagName, null, null);
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
        return Git.getInstance().checkout(repository, currentBranch, null, true, false);
    }

    private class MyMergeConflictResolver extends GitMergeCommittingConflictResolver {
        String currentBranch;
        String targetBranch;

        MyMergeConflictResolver(GitRepository repository, String currentBranch, String targetBranch) {
            super(repository.getProject(), Git.getInstance(), new GitMerger(repository.getProject()),
                    GitUtil.getRootsFromRepositories(Lists.newArrayList(repository)), new Params(repository.getProject()), true);
            this.currentBranch = currentBranch;
            this.targetBranch = targetBranch;
        }

        @Override
        protected void notifyUnresolvedRemain() {
            notifyWarning("合并代码冲突", String.format("%s 分支合并到 %s分支发生代码冲突", currentBranch, targetBranch));
        }
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
        GitCommandResult result = Git.getInstance().runCommand(h);

        if (result.success() && isNewBranch(result)) {
            return true;
        }

        return false;
    }

    private boolean isNewBranch(GitCommandResult result) {
        return result.getOutputAsJoinedString().contains("new branch") || result.getErrorOutputAsJoinedString().contains("new branch");
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
        return Git.getInstance().runCommand(h);
    }

    private GitCommandResult push(GitRepository repository, String branchName, GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(branchName + ":" + branchName);
        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        return Git.getInstance().runCommand(h);
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

        return Git.getInstance().runCommand(h);
    }

    private GitRemote getDefaultRemote(@NotNull GitRepository repository) {
        Collection<GitRemote> remotes = repository.getRemotes();
        if (CollectionUtils.isEmpty(remotes)) {
            return null;
        }
        return remotes.iterator().next();
    }
}
