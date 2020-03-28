package com.github.xiaolyuh.action;

import com.github.xiaolyuh.MrtfGitFlow;
import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.listener.ErrorsListener;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.Objects;

/**
 * Merge 抽象Action
 *
 * @author yuhao.wang3
 */
public abstract class AbstractMergeAction extends AnAction {
    protected MrtfGitFlow mrtfGitFlow = MrtfGitFlow.getInstance();

    public AbstractMergeAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        if (Objects.isNull(event.getProject())) {
            event.getPresentation().setEnabled(false);
            return;
        }
        boolean isInit = GitBranchUtil.isGitProject(event.getProject()) && ConfigUtil.isInit(event.getProject());
        if (!isInit) {
            event.getPresentation().setEnabled(false);
            return;
        }

        String currentBranch = getCurrentBranch(event.getProject());
        String featurePrefix = ConfigUtil.getConfig(event.getProject()).get().getFeaturePrefix();
        String hotfixPrefix = ConfigUtil.getConfig(event.getProject()).get().getHotfixPrefix();
        // 已经初始化并且前缀是开发分支才显示
        boolean isDevBranch = StringUtils.startsWith(currentBranch, featurePrefix) || StringUtils.startsWith(currentBranch, hotfixPrefix);
        event.getPresentation().setEnabled(isDevBranch && !isConflicts(event.getProject()));
    }

    /**
     * 代码是否存在冲突
     *
     * @param project project
     * @return 是=true
     */
    boolean isConflicts(@NotNull Project project) {
        Collection<Change> changes = ChangeListManager.getInstance(project).getAllChanges();
        if (changes.size() > 1000) {
            return true;
        }
        return changes.stream().anyMatch(it -> it.getFileStatus() == FileStatus.MERGED_WITH_CONFLICTS);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        actionPerformed(event, null);
    }

    void actionPerformed(@NotNull AnActionEvent event, TagOptions tagOptions) {
        Project project = event.getProject();
        String currentBranch = getCurrentBranch(project);
        String targetBranch = getTargetBranch(project);

        int flag = Messages.showOkCancelDialog(project, getDialogContent(project),
                getDialogTitle(project), IconLoader.getIcon("/icons/warning.svg"));
        if (flag == 0) {
            new Task.Backgroundable(project, getTaskTitle(project), false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    GitRepository repository = GitBranchUtil.getCurrentRepository(project);
                    if (Objects.isNull(repository)) {
                        return;
                    }

                    // 加锁
                    if (isLock() && !mrtfGitFlow.lock(repository, getCurrentBranch(project))) {
                        String msg = mrtfGitFlow.getRemoteLastCommit(repository, getTargetBranch(project));
                        NotifyUtil.notifyError(project, "Error",
                                String.format("发布分支已被锁定，最后一次操作：%s ;\r\n如需强行发布，请先点[发布失败]解除锁定，再点[开始发布]。", msg));
                        return;
                    }

                    // 开始合并分支
                    if (isMerge()) {
                        ErrorsListener errorListener = new ErrorsListener(project);
                        GitCommandResult result = mrtfGitFlow.mergeBranchAndPush(repository, currentBranch, targetBranch, tagOptions, errorListener);
                        if (result.success()) {
                            NotifyUtil.notifySuccess(myProject, "Success", String.format("%s 分支已经合并到了 %s 分支，并推送到了远程仓库", currentBranch, targetBranch));
                            // 钉钉通知
                            if (isLock()) {
                                mrtfGitFlow.thirdPartyNotify(repository);
                            }
                        } else {
                            NotifyUtil.notifyError(myProject, "Error", result.getErrorOutputAsJoinedString());
                        }
                    }

                    // 解锁
                    if (isUnLock()) {
                        mrtfGitFlow.unlock(repository);
                    }

                    // 刷新
                    repository.update();
                    myProject.getMessageBus().syncPublisher(GitRepository.GIT_REPO_CHANGE).repositoryChanged(repository);
                    VirtualFileManager.getInstance().asyncRefresh(null);
                }
            }.queue();
        }
    }

    /**
     * 获取目标分支
     *
     * @param project project
     * @return String
     */
    protected abstract String getTargetBranch(Project project);

    /**
     * 获取当前分支
     *
     * @param project project
     * @return String
     */
    protected String getCurrentBranch(Project project) {
        return mrtfGitFlow.getCurrentBranch(project);
    }

    /**
     * 获取标题
     *
     * @param project project
     * @return String
     */
    protected abstract String getDialogTitle(Project project);

    /**
     * 获取弹框内容
     *
     * @param project project
     * @return String
     */
    protected String getDialogContent(Project project) {
        return String.format("你是否确认将 %s 分支，合并到 %s 分支？", getCurrentBranch(project), getTargetBranch(project));
    }

    /**
     * 获取Task标题
     *
     * @param project project
     * @return String
     */
    protected abstract String getTaskTitle(Project project);

    /**
     * 是否需要加锁
     *
     * @return boolean
     */
    protected boolean isLock() {
        return false;
    }

    /**
     * 是否需要获解锁
     *
     * @return boolean
     */
    protected boolean isUnLock() {
        return false;
    }

    /**
     * 是否需要合并分支
     *
     * @return boolean
     */
    protected boolean isMerge() {
        return true;
    }
}
