package com.github.xiaolyuh.ui;

import com.github.xiaolyuh.action.*;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.ui.ClickListener;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.PopupFactoryImpl;
import git4idea.actions.GitResolveConflictsAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * mrtf git flow 状态栏小部件
 *
 * @author yuhao.wang3
 */
public class MrtfGitFlowWidget extends EditorBasedWidget implements StatusBarWidget.Multiframe, CustomStatusBarWidget {
    private final TextPanel.WithIconAndArrows myComponent;
    DefaultActionGroup popupGroup;
    Project project;


    public MrtfGitFlowWidget(@NotNull Project project) {
        super(project);
        this.project = project;

        initPopupGroup();
        myComponent = new TextPanel.WithIconAndArrows() {
        };

        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                update();
                showPopup(e);
                return true;
            }
        }.installOn(myComponent);
        myComponent.setBorder(WidgetBorder.WIDE);
    }

    private void showPopup(MouseEvent e) {
        ListPopup popup = new PopupFactoryImpl.ActionGroupPopup("Mrtf - Git - Flow ...", popupGroup, SimpleDataContext.getProjectContext(project),
                false, false, true, true, null, -1, null, null);

        if (popup != null) {
            Dimension dimension = popup.getContent().getPreferredSize();
            Point at = new Point(0, -dimension.height);
            popup.show(new RelativePoint(e.getComponent(), at));
            Disposer.register(this, popup); // destroy popup on unexpected project close
        }
    }

    public void update() {
        myComponent.setVisible(true);
        myComponent.setToolTipText("MrtfGitFlow");
        myComponent.setText("MrtfGitFlow");
        myComponent.invalidate();
        if (myStatusBar != null) {
            myStatusBar.updateWidget(ID());
        }
    }

    private void initPopupGroup() {
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


    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Override
    public StatusBarWidget copy() {
        return new MrtfGitFlowWidget(project);
    }

    @NotNull
    @Override
    public String ID() {
        return MrtfGitFlowWidget.class.getName();
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation(@NotNull PlatformType type) {
        return null;
    }
}
