package com.github.xiaolyuh.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.IconLoader;

/**
 * 帮助
 *
 * @author yuhao.wang3
 */
public class HelpAction extends AnAction {
    public HelpAction() {
        super("帮助", "帮助", IconLoader.getIcon("/icons/help.svg"));
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        BrowserUtil.browse("https://github.com/xiaolyuh/mrtf-git-flow-4idea");
    }
}
