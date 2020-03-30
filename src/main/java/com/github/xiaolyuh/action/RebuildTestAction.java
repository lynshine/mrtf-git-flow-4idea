package com.github.xiaolyuh.action;

import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.StringUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;

/**
 * 重建测试分支
 *
 * @author yuhao.wang3
 */
public class RebuildTestAction extends AbstractNewBranchAction {

    public RebuildTestAction() {
        super("重建测试分支", "重建测试分支，并推送到远程仓库，原来的分支将被删除",
                IconLoader.getIcon("/icons/test.svg"));
    }

    @Override
    public String getPrefix(Project project) {
        return StringUtils.EMPTY;
    }

    @Override
    public String getInputString(Project project) {
        String test = ConfigUtil.getConfig(project).get().getTestBranch();
        int flag = Messages.showOkCancelDialog(project, String.format("你是否需要重建 %s 分支，原来的 %s 分支将会被删除!", test, test),
                "创建测试分支", "确认", "取消", IconLoader.getIcon("/icons/warning.svg"));

        return flag == 0 ? test : StringUtils.EMPTY;
    }

    @Override
    public String getTitle(String branchName) {
        return "正在创建测试分支: " + branchName;
    }
}
