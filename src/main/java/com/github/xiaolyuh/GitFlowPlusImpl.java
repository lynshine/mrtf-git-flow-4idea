package com.github.xiaolyuh;

import com.github.xiaolyuh.utils.*;
import com.google.common.collect.Lists;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitUtil;
import git4idea.commands.*;
import git4idea.i18n.GitBundle;
import git4idea.merge.GitMergeCommittingConflictResolver;
import git4idea.merge.GitMerger;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.util.GitFileUtils;
import git4idea.util.GitUIUtil;
import git4idea.util.StringScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yuhao.wang3
 * @since 2020/3/23 9:53
 */
public class GitFlowPlusImpl implements GitFlowPlus {
    private Git git = Git.getInstance();

    @Override
    public void addConfigToGit(GitRepository repository) {
        try {
            String filePath = repository.getProject().getBasePath() + File.separator + Constants.CONFIG_FILE_NAME;
            FilePath path = VcsUtil.getFilePath(filePath);
            GitFileUtils.addPaths(repository.getProject(), repository.getRoot(), Lists.newArrayList(path));
        } catch (VcsException e) {

        }
    }

    @Override
    public GitCommandResult newNewBranchBaseRemoteMaster(@NotNull GitRepository repository, @Nullable String master, @NotNull String newBranchName, @Nullable GitLineHandlerListener... listeners) {
        newBranch(repository, master, newBranchName, listeners);

        // 切换分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false checkout %s --force", newBranchName));
        git.checkout(repository, newBranchName, null, true, false);

        // 推送分支
        return push(repository, newBranchName, true, listeners);
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

        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false checkout %s --force", master));
        git.checkout(repository, master, null, true, false);

        deleteRemoteBranch(repository, branchName, listener);

        // 删除本地分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false branch -D %s", branchName));
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
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
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
        String releaseBranch = ReadAction.compute(() -> ConfigUtil.getConfig(repository.getProject()).get().getReleaseBranch());
        // 判断目标分支是否存在
        GitCommandResult result = checkTargetBranchIsExist(repository, targetBranch, errorListener);
        if (Objects.nonNull(result) && !result.success()) {
            return result;
        }

        // 发布完成打拉取release最新代码
        if (Objects.nonNull(tagOptions)) {
            result = checkoutTargetBranchAndPull(repository, releaseBranch, errorListener);
            if (!result.success()) {
                return result;
            }
        }

        // 切换到目标分支, pull最新代码
        result = checkoutTargetBranchAndPull(repository, targetBranch, errorListener);
        if (!result.success()) {
            return result;
        }

        // 合并代码
        GitSimpleEventDetector mergeConflict = new GitSimpleEventDetector(GitSimpleEventDetector.Event.MERGE_CONFLICT);
        String branchToMerge = Objects.nonNull(tagOptions) ? releaseBranch : currentBranch;
        NotifyUtil.notifyGitCommand(repository.getProject(),
                String.format("git -c core.quotepath=false -c log.showSignature=false merge %s ['%s' merge into '%s']", branchToMerge, branchToMerge, repository.getCurrentBranch()));

        result = git.merge(repository, branchToMerge, Lists.newArrayList(), errorListener, mergeConflict);

        boolean allConflictsResolved = true;
        if (mergeConflict.hasHappened()) {
            // 解决冲突
            allConflictsResolved = new MyMergeConflictResolver(repository, currentBranch, targetBranch).merge();
        }

        if (!result.success() && !allConflictsResolved) {
            return result;
        }

        // 发布完成打tag
        if (Objects.nonNull(tagOptions)) {
            result = createNewTag(repository, tagOptions.getTagName(), tagOptions.getMessage(), errorListener);
            if (!result.success()) {
                return result;
            }
        }

        // push代码
        result = push(repository, targetBranch, false, errorListener);
        if (!result.success()) {
            return result;
        }

        // 切换到当前分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false checkout %s --force", currentBranch));
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

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
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
    public boolean isLock(GitRepository repository) {
        GitRemote remote = getDefaultRemote(repository);
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false fetch origin"));
        git.fetch(repository, remote, Collections.singletonList(new GitFetchPruneDetector()), new String[0]);
        repository.update();

        return isLock(repository.getProject());
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

    @Override
    public boolean isExistChangeFile(@NotNull Project project) {
        Collection<Change> changes = ChangeListManager.getInstance(project).getAllChanges();
        if (CollectionUtils.isNotEmpty(changes)) {
            StringBuffer builder = new StringBuffer();
            changes.parallelStream().forEach(change -> builder.append(change.toString() + "\r\n"));
            NotifyUtil.notifyError(project, "Error", String.format("当前分支存在未提交的文件:\r\n %s", builder.toString()));
            return true;
        }
        return false;
    }


    @Override
    public String getUserEmail(GitRepository repository) {
        try {
            GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.CONFIG);
            h.setSilent(true);
            h.addParameters("--null", "--get", "user.email");
            NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
            GitCommandResult result = Git.getInstance().runCommand(h);
            String output = result.getOutputOrThrow(1);
            int pos = output.indexOf('\u0000');
            if (result.getExitCode() != 0 || pos == -1) {
                return "";
            }
            return output.substring(0, pos);
        } catch (VcsException e) {
            return "";
        }
    }

    @Override
    public boolean isExistTag(GitRepository repository, String tagName) {
        Set<String> myExistingTags = new HashSet<>();
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.TAG);
        h.setSilent(true);
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());

        GitCommandResult result = ProgressManager.getInstance()
                .runProcessWithProgressSynchronously(() -> Git.getInstance().runCommand(h),
                        GitBundle.getString("tag.getting.existing.tags"),
                        false,
                        repository.getProject());
        if (!result.success()) {
            GitUIUtil.showOperationError(repository.getProject(), GitBundle.getString("tag.getting.existing.tags"), result.getErrorOutputAsJoinedString());
            throw new ProcessCanceledException();
        }
        for (StringScanner s = new StringScanner(result.getOutputAsJoinedString()); s.hasMoreData(); ) {
            String line = s.line();
            if (line.length() == 0) {
                continue;
            }
            myExistingTags.add(line);
        }

        return myExistingTags.contains(tagName);
    }


    private GitCommandResult checkTargetBranchIsExist(GitRepository repository, String
            targetBranch, GitLineHandlerListener errorListener) {
        // 判断本地是否存在分支
        if (!GitBranchUtil.getLocalBranches(repository.getProject()).contains(targetBranch)) {
            if (GitBranchUtil.getRemoteBranches(repository.getProject()).contains(targetBranch)) {
                return checkoutNewBranch(repository, targetBranch, errorListener);
            } else {
                String master = ConfigUtil.getConfig(repository.getProject()).get().getMasterBranch();
                return newNewBranchBaseRemoteMaster(repository, master, targetBranch, errorListener);
            }
        }

        return null;
    }

    private GitCommandResult checkoutNewBranch(GitRepository repository, String
            targetBranch, GitLineHandlerListener errorListener) {
        // git checkout -b 本地分支名x origin/远程分支名x
        final GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.CHECKOUT);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.addParameters("-b");
        h.addParameters(targetBranch);
        h.addParameters("origin/" + targetBranch);
        if (errorListener != null) {
            h.addLineListener(errorListener);
        }
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
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
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    private GitCommandResult pull(GitRepository repository, @Nullable String
            branchName, @Nullable GitLineHandlerListener... listeners) {
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

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    private GitCommandResult push(GitRepository repository, String branchName,
                                  boolean isNewBranch, GitLineHandlerListener... listeners) {
        GitRemote remote = getDefaultRemote(repository);
        GitLineHandler h = new GitLineHandler(repository.getProject(), repository.getRoot(), GitCommand.PUSH);
        h.setSilent(false);
        h.setStdoutSuppressed(false);
        h.setUrls(remote.getUrls());
        h.addParameters("origin");
        h.addParameters(branchName + ":" + branchName);
        h.addParameters("--tag");
        if (isNewBranch) {
            h.addParameters("--set-upstream");
        }
        for (GitLineHandlerListener listener : listeners) {
            h.addLineListener(listener);
        }
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
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
    private GitCommandResult deleteRemoteBranch(@NotNull GitRepository repository, @Nullable String
            branchName, @Nullable GitLineHandlerListener... listeners) {
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
        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
        return git.runCommand(h);
    }

    private GitCommandResult newBranch(GitRepository repository, String master, String newBranchName, GitLineHandlerListener... listeners) {
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

        NotifyUtil.notifyGitCommand(repository.getProject(), h.printableCommandLine());
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

    private GitCommandResult checkoutTargetBranchAndPull(GitRepository repository, String targetBranch, GitLineHandlerListener errorListener) {
        // 切换到目标分支
        NotifyUtil.notifyGitCommand(repository.getProject(), String.format("git -c core.quotepath=false -c log.showSignature=false checkout %s --force", targetBranch));
        git.checkout(repository, targetBranch, null, true, false);

        // pull最新代码
        return pull(repository, targetBranch, errorListener);
    }

    private class MyMergeConflictResolver extends GitMergeCommittingConflictResolver {
        String currentBranch;
        String targetBranch;

        MyMergeConflictResolver(GitRepository repository, String currentBranch, String targetBranch) {
            super(repository.getProject(), git, new GitMerger(repository.getProject()),
                    GitUtil.getRootsFromRepositories(Lists.newArrayList(repository)), new Params(), true);
            this.currentBranch = currentBranch;
            this.targetBranch = targetBranch;
        }

        @Override
        protected void notifyUnresolvedRemain() {
            notifyWarning("合并代码冲突", String.format("%s 分支合并到 %s分支发生代码冲突", currentBranch, targetBranch));
        }
    }

    private class GitFetchPruneDetector extends GitLineHandlerAdapter {

        private final Pattern PRUNE_PATTERN = Pattern.compile("\\s*x\\s*\\[deleted\\].*->\\s*(\\S*)");

        @NotNull
        private final Collection<String> myPrunedRefs = new ArrayList<>();

        @Override
        public void onLineAvailable(String line, Key outputType) {
            //  x [deleted]         (none)     -> origin/frmari
            Matcher matcher = PRUNE_PATTERN.matcher(line);
            if (matcher.matches()) {
                myPrunedRefs.add(matcher.group(1));
            }
        }

        @NotNull
        public Collection<String> getPrunedRefs() {
            return myPrunedRefs;
        }
    }
}
