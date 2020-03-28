package com.github.xiaolyuh.listener;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import git4idea.commands.GitLineHandlerListener;

/**
 * 异常监听器
 *
 * @author yuhao.wang3
 */
public class ErrorsListener implements GitLineHandlerListener {

    boolean hasMergeError = false;
    Project myProject;

    public ErrorsListener(Project project) {
        myProject = project;
    }

    @Override
    public void onLineAvailable(String line, Key outputType) {
        if (line.contains("There were merge conflicts")) {
            hasMergeError = true;
        }
    }

}