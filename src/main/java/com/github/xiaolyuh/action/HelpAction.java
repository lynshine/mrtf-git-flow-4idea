package com.github.xiaolyuh.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * 帮助
 *
 * @author yuhao.wang3
 */
public class HelpAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {

        BrowserUtil.browse("https://github.com/xiaolyuh/mrtf-git-flow-4idea");
    }
}
