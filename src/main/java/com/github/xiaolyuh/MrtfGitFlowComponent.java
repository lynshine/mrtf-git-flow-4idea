package com.github.xiaolyuh;

import com.github.xiaolyuh.ui.MrtfGitFlowWidget;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.vcs.VcsRoot;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.messages.MessageBus;
import git4idea.GitVcs;
import git4idea.ui.branch.GitBranchWidget;
import org.jetbrains.annotations.NotNull;


/**
 * Gitflow 组件
 *
 * @author yuhao.wang3
 */
public class MrtfGitFlowComponent implements ProjectComponent, VcsListener {
    Project project;
    MrtfGitFlowWidget mrtfGitFlowWidget;
    MessageBus messageBus;

    public MrtfGitFlowComponent(Project project) {
        this.project = project;
    }

    @Override
    public void initComponent() {
        messageBus = project.getMessageBus();
        messageBus.connect().subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, this);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return MrtfGitFlowComponent.class.getSimpleName();
    }

    @Override
    public void directoryMappingChanged() {
        VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        //git repo present
        if (vcsRoots.length > 0 && vcsRoots[0].getVcs() instanceof GitVcs) {
            //make sure to not reinitialize the widget if it's already present
            if (mrtfGitFlowWidget == null) {
                mrtfGitFlowWidget = new MrtfGitFlowWidget(project);

                statusBar.addWidget(mrtfGitFlowWidget, "after " + GitBranchWidget.class.getName(), project);
                mrtfGitFlowWidget.update();
            }
        } else {
            if (mrtfGitFlowWidget != null) {
                mrtfGitFlowWidget.dispose();
            }
            mrtfGitFlowWidget = null;
        }
    }
}
