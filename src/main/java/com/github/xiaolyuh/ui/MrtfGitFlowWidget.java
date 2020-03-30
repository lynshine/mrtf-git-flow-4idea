package com.github.xiaolyuh.ui;

import com.github.xiaolyuh.action.*;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.ui.popup.PopupFactoryImpl;
import git4idea.actions.GitResolveConflictsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * mrtf git flow 状态栏小部件
 *
 * @author yuhao.wang3
 */
public class MrtfGitFlowWidget extends EditorBasedStatusBarPopup {

    DefaultActionGroup popupGroup;
    Project project;


    public MrtfGitFlowWidget(@NotNull Project project) {
        super(project);
        this.project = project;

        //No advanced features in the status-bar widget
        popupGroup = new DefaultActionGroup();
        popupGroup.add(new InitPluginAction());
        popupGroup.add(new Separator());

        popupGroup.add(new NewFeatureAction());
        popupGroup.add(new NewHotFixAction());
        popupGroup.add(new Separator());

        DefaultActionGroup rebuildPopupGroup = new DefaultActionGroup("重建分支", true);
        rebuildPopupGroup.add(new RebuildTestAction());
        rebuildPopupGroup.add(new RebuildReleaseAction());
        popupGroup.add(rebuildPopupGroup);
        popupGroup.add(new Separator());

        GitResolveConflictsAction conflictsAction = new GitResolveConflictsAction();
        conflictsAction.getTemplatePresentation().setText("解决冲突");
        popupGroup.add(conflictsAction);
        popupGroup.add(new Separator());

        popupGroup.add(new StartTestAction());
        popupGroup.add(new Separator());

        popupGroup.add(new StartReleaseAction());
        popupGroup.add(new FinishReleaseAction());
        popupGroup.add(new FailureReleaseAction());
        popupGroup.add(new Separator());

        popupGroup.add(new HelpAction());
    }


    @NotNull
    @Override
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        return new WidgetState("MrtfGitFlow", "MrtfGitFlow", true);
    }

    @Nullable
    @Override
    protected ListPopup createPopup(DataContext context) {
        if (project == null) {
            return null;
        }

        return new PopupFactoryImpl.ActionGroupPopup("Mrtf-Git-Flow ...", popupGroup, SimpleDataContext.getProjectContext(project), false, false, true, true, null, -1,
                null, null);
    }

    @Override
    protected void registerCustomListeners() {

    }

    @NotNull
    @Override
    protected StatusBarWidget createInstance(Project project) {
        return new MrtfGitFlowWidget(project);
    }

    @NotNull
    @Override
    public String ID() {
        return MrtfGitFlowWidget.class.getName();
    }
}
