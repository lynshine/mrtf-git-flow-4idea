package com.github.xiaolyuh.action;

import com.github.xiaolyuh.Constants;
import com.github.xiaolyuh.GitNewBranchNameValidator;
import com.github.xiaolyuh.GitNewTagValidator;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 发布完成
 *
 * @author yuhao.wang3
 */
public class FinishReleaseAction extends AbstractMergeAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabled()) {
            event.getPresentation().setEnabled(GitBranchUtil.getRemoteBranches(event.getProject()).contains(Constants.CONFIG_FILE_NAME));
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        String tagName = Messages.showMultilineInputDialog(project, "请输入Tag名称M", "请输入Tag名称T", "", null,
                GitNewTagValidator.newInstance());

        System.out.println(tagName);
        if (StringUtils.isNotBlank(tagName)) {
            super.actionPerformed(event);
        }
    }

    @Override
    protected String getTargetBranch(Project project) {
        return ConfigUtil.getConfig(project).get().getMasterBranch();
    }

    @Override
    protected String getDialogTitle(Project project) {
        return "发布成功";
    }

    @Override
    protected String getTaskTitle(Project project) {
        String release = ConfigUtil.getConfig(project).get().getReleaseBranch();
        return String.format("将 %s 分支，合并到 %s 分支", release, getTargetBranch(project));
    }

    @Override
    protected boolean isUnLock() {
        return true;
    }
}
