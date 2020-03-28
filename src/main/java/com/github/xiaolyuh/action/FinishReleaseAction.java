package com.github.xiaolyuh.action;

import com.github.xiaolyuh.ui.TagDialog;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
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
            event.getPresentation().setEnabled(mrtfGitFlow.isLock(event.getProject()));
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        TagDialog tagDialog = new TagDialog(project);
        tagDialog.show();
        if (tagDialog.isOK()) {
            super.actionPerformed(event, tagDialog.getTagOptions());
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
