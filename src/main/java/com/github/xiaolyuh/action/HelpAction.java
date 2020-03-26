package com.github.xiaolyuh.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import git4idea.actions.GitResolveConflictsAction;
import org.jetbrains.annotations.NotNull;

/**
 * 帮助
 *
 * @author yuhao.wang3
 */
public class HelpAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        GitResolveConflictsAction action = new GitResolveConflictsAction();
        action.actionPerformed(event);
    }
}
