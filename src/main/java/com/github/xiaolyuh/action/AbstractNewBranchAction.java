package com.github.xiaolyuh.action;

import com.github.xiaolyuh.MrtfGitFlow;
import com.github.xiaolyuh.listener.ErrorsListener;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

/**
 * 新建分支
 *
 * @author yuhao.wang3
 */
public abstract class AbstractNewBranchAction extends AnAction {
    MrtfGitFlow mrtfGitFlow = MrtfGitFlow.getInstance();

    public AbstractNewBranchAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        Project project = event.getProject();
        event.getPresentation().setEnabled(GitBranchUtil.isGitProject(project) && ConfigUtil.isInit(project));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String featurePrefix = getPrefix(project);
        // 获取输入框内容
        String inputString = getInputString(project);
        if (StringUtils.isBlank(inputString)) {
            return;
        }

        // 获取开发分支完整名称
        final String newBranchName = featurePrefix + inputString;
        new Task.Backgroundable(project, getTitle(newBranchName), false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                final String master = ConfigUtil.getConfig(project).get().getMasterBranch();
                ErrorsListener errorListener = new ErrorsListener(project);
                GitRepository repository = GitBranchUtil.getCurrentRepository(project);
                if (Objects.isNull(repository)) {
                    return;
                }
                if (isDeleteBranch()) {
                    // 删除分支
                    GitCommandResult result = mrtfGitFlow.deleteBranch(repository, master, newBranchName, errorListener);
                    if (result.success()) {
                        NotifyUtil.notifySuccess(myProject, "Success", String.format("%s 删除成功", newBranchName));
                    } else {
                        NotifyUtil.notifyError(myProject, "Error", "删除分支异常：" + result.getErrorOutputAsJoinedString());
                    }
                }

                // 新建分支
                GitCommandResult result = mrtfGitFlow.newNewBranchBaseRemoteMaster(repository, master, newBranchName, errorListener);
                if (result.success()) {
                    NotifyUtil.notifySuccess(myProject, "Success", String.format("基于 origin/%s 成功创建分支 %s ", master, newBranchName));
                } else {
                    NotifyUtil.notifyError(myProject, "Error", result.getErrorOutputAsJoinedString());
                }

                // 刷新
                repository.update();
                VirtualFileManager.getInstance().asyncRefresh(null);
            }
        }.queue();

    }

    /**
     * 获取分支前缀
     *
     * @param project Project
     * @return String
     */
    abstract public String getPrefix(Project project);

    /**
     * 获取输入的分支名称
     *
     * @param project Project
     * @return String
     */
    abstract public String getInputString(Project project);

    /**
     * 获取标题
     *
     * @param branchName branchName
     * @return String
     */
    abstract public String getTitle(String branchName);

    /**
     * 是否先删除分支
     *
     * @return boolean
     */
    public boolean isDeleteBranch() {
        return true;
    }

}
