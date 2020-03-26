package com.github.xiaolyuh.action;

import com.github.xiaolyuh.utils.ConfigUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.IconLoader;
import org.apache.commons.lang3.StringUtils;

/**
 * 新建发布分支
 *
 * @author yuhao.wang3
 */
public class NewReleaseAction extends AbstractNewBranchAction {

    @Override
    public String getPrefix(Project project) {
        return StringUtils.EMPTY;
    }

    @Override
    public String getInputString(Project project) {
        String release = ConfigUtil.getConfig(project).get().getReleaseBranch();
        int flag = Messages.showOkCancelDialog(project, String.format("你是否需要重建 %s 分支，原来的 %s 分支将会被删除!", release, release),
                "创建发布分支", IconLoader.getIcon("/icons/warning.svg"));

        return flag == 0 ? release : StringUtils.EMPTY;
    }

    @Override
    public String getTitle(String branchName) {
        return "正在创建发布分支: " + branchName;
    }
}
