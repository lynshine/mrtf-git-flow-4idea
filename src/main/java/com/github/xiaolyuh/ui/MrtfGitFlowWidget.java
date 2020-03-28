package com.github.xiaolyuh.ui;

import com.github.xiaolyuh.action.*;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.Consumer;
import git4idea.actions.GitResolveConflictsAction;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryChangeListener;
import git4idea.ui.branch.GitBranchWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

/**
 * mrtf git flow 状态栏小部件
 *
 * @author yuhao.wang3
 */
public class MrtfGitFlowWidget extends GitBranchWidget implements GitRepositoryChangeListener {

    DefaultActionGroup popupGroup;

    public MrtfGitFlowWidget(@NotNull Project project) {
        super(project);
        project.getMessageBus().connect().subscribe(GitRepository.GIT_REPO_CHANGE, this);

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


        updateAsync();
    }

    @Override
    public void repositoryChanged(@NotNull GitRepository repository) {
        updateAsync();
    }

    @Override
    public ListPopup getPopupStep() {
        Project project = getProject();
        if (project == null) {
            return null;
        }
        GitRepository repo = GitBranchUtil.getCurrentRepository(project);
        if (repo == null) {
            return null;
        }


        ListPopup listPopup = new PopupFactoryImpl.ActionGroupPopup("Mrtf-Git-Flow Plugin", popupGroup, SimpleDataContext.getProjectContext(project), false, false, true, true, null, -1,
                null, null);

        return listPopup;
    }

    @Override
    public String getSelectedValue() {
        return "MrtfGitFlow";
    }

    @Nullable
    @Override
    public String getTooltipText() {
        return "MrtfGitFlow";
    }

    /**
     * Updates branch information on click
     *
     * @return
     */
    @Override
    public Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> updateAsync();
    }

    private void updateAsync() {
        ApplicationManager.getApplication().invokeLater(this::update);
    }

    private void update() {
        Project project = getProject();
        if (project == null) {
            return;
        }

        GitRepository repo = GitBranchUtil.getCurrentRepository(project);
        if (repo == null) {
            return;
        }

        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    @NotNull
    @Override
    public String ID() {
        return MrtfGitFlowWidget.class.getName();
    }

}
