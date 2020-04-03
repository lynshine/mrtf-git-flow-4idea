package com.github.xiaolyuh.valve.merge;

import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;

/**
 * 加锁阀门
 *
 * @author yuhao.wang3
 * @since 2020/4/7 16:42
 */
public class LockValve extends Valve {
    private static LockValve lockValve = new LockValve();

    public static Valve getInstance() {
        return lockValve;
    }

    @Override
    public boolean invoke(Project project, GitRepository repository, String currentBranch, String targetBranch, TagOptions tagOptions) {
        if (!gitFlowPlus.lock(repository, currentBranch)) {
            String msg = gitFlowPlus.getRemoteLastCommit(repository, targetBranch);
            NotifyUtil.notifyError(project, "Error",
                    String.format("发布分支已被锁定，最后一次操作：%s ;\r\n如需强行发布，请先点[发布失败]解除锁定，再点[开始发布]。", msg));
            return false;
        }
        return true;
    }
}
