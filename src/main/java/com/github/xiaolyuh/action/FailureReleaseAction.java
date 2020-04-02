package com.github.xiaolyuh.action;

import com.github.xiaolyuh.utils.ConfigUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

/**
 * 发布失败
 *
 * @author yuhao.wang3
 */
public class FailureReleaseAction extends AbstractMergeAction {

    public FailureReleaseAction() {
        super("发布失败", "解锁，这时新的开发分支可以合并到发布分支了", IconLoader.getIcon("/icons/failure.svg"));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        if (event.getPresentation().isEnabled()) {
            event.getPresentation().setEnabled(gitFlowPlus.isLock(event.getProject()));
        }
    }


    @Override
    protected String getTargetBranch(Project project) {
        return ConfigUtil.getConfig(project).get().getMasterBranch();
    }

    @Override
    protected String getDialogTitle(Project project) {
        return "发布失败";
    }

    @Override
    protected String getDialogContent(Project project) {
        String release = ConfigUtil.getConfig(project).get().getReleaseBranch();
        return String.format("发布失败将会解除 %s 分支的锁定", release);
    }

    @Override
    protected String getTaskTitle(Project project) {
        String release = ConfigUtil.getConfig(project).get().getReleaseBranch();
        return String.format("正在解除 %s 分支的锁定", release);
    }

    @Override
    protected boolean isUnLock() {
        return true;
    }

    @Override
    protected boolean isMerge() {
        return false;
    }
}
