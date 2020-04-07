package com.github.xiaolyuh;

import com.github.xiaolyuh.ui.GitFlowPlusWidget;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationNamesInfo;
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


/**
 * Gitflow 组件
 *
 * @author yuhao.wang3
 */
public class GitFlowPlusComponent implements ProjectComponent, VcsListener {
    Project project;
    GitFlowPlusWidget gitFlowPlusWidget;
    MessageBus messageBus;

    public GitFlowPlusComponent(Project project) {
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
        return GitFlowPlusComponent.class.getSimpleName();
    }

    @Override
    public void directoryMappingChanged() {
        VcsRoot[] vcsRoots = ProjectLevelVcsManager.getInstance(project).getAllVcsRoots();
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        //git repo present
        if (vcsRoots.length > 0 && vcsRoots[0].getVcs() instanceof GitVcs) {
            //make sure to not reinitialize the widget if it's already present
            if (gitFlowPlusWidget == null) {
                gitFlowPlusWidget = new GitFlowPlusWidget(project);

                statusBar.addWidget(gitFlowPlusWidget, "after " + GitBranchWidget.class.getName(), project);
                gitFlowPlusWidget.update();
            }
        } else {
            if (gitFlowPlusWidget != null) {
                gitFlowPlusWidget.dispose();
            }
            gitFlowPlusWidget = null;
        }
    }
}
