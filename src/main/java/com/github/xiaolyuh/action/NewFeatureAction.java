package com.github.xiaolyuh.action;

import com.github.xiaolyuh.GitNewBranchNameValidator;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import git4idea.GitUtil;

/**
 * 新建开发分支
 *
 * @author yuhao.wang3
 */
public class NewFeatureAction extends AbstractNewBranchAction {

    @Override
    public String getPrefix(Project project) {
        return ConfigUtil.getConfig(project).get().getFeaturePrefix();
    }

    @Override
    public String getInputString(Project project) {
        return Messages.showInputDialog(project, "请输入开发分支名称", "新建开发分支", null, "",
                GitNewBranchNameValidator.newInstance(GitUtil.getRepositoryManager(project).getRepositories(), getPrefix(project)));
    }

    @Override
    public String getTitle(String branchName) {
        return "正在创建开发分支: " + branchName;
    }

    @Override
    public boolean isDeleteBranch() {
        return false;
    }
}
