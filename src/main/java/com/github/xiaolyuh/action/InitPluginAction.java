package com.github.xiaolyuh.action;

import com.alibaba.fastjson.JSON;
import com.github.xiaolyuh.InitOptions;
import com.github.xiaolyuh.MrtfGitFlow;
import com.github.xiaolyuh.listener.ErrorsListener;
import com.github.xiaolyuh.ui.InitPluginDialog;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.NotifyUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFileManager;
import git4idea.commands.GitCommandResult;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * 初始化Action
 *
 * @author yuhao.wang3
 */
public class InitPluginAction extends AnAction {

    private MrtfGitFlow mrtfGitFlow = MrtfGitFlow.getInstance();

    public InitPluginAction() {
        super("初始化配置", "初始化仓库配置，如果测试分支与发布分支不存在，将基于master新建", IconLoader.getIcon("/icons/config.svg"));
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        super.update(event);
        event.getPresentation().setEnabledAndVisible(GitBranchUtil.isGitProject(event.getProject()));
        event.getPresentation().setText(ConfigUtil.isInit(event.getProject()) ? "更新配置" : "初始配置");
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Project project = event.getProject();
        final GitRepository repository = GitBranchUtil.getCurrentRepository(project);
        if (Objects.isNull(repository)) {
            return;
        }

        InitPluginDialog initPluginDialog = new InitPluginDialog(project);
        initPluginDialog.show();

        if (initPluginDialog.isOK()) {
            final InitOptions initOptions = initPluginDialog.getOptions();

            new Task.Backgroundable(project, "Init MrtfGitFlow Plugins", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    // 校验主干分支是否存在
                    List<String> remoteBranches = GitBranchUtil.getRemoteBranches(project);
                    if (!remoteBranches.contains(initOptions.getMasterBranch())) {
                        NotifyUtil.notifyError(myProject, "Error", String.format("远程仓库中没有 %s 分支，MrtfGitFlow初始化失败", initOptions.getMasterBranch()));
                        return;
                    }

                    // 校验主测试支是否存在，不存在就新建
                    if (!remoteBranches.contains(initOptions.getTestBranch())) {
                        ErrorsListener errorListener = new ErrorsListener(project);
                        GitCommandResult result = mrtfGitFlow.newNewBranchBaseRemoteMaster(repository, initOptions.getMasterBranch(), initOptions.getTestBranch(), errorListener);
                        if (result.success()) {
                            NotifyUtil.notifySuccess(myProject, "Success", String.format("基于 origin/%s 成功创建分支 %s ", initOptions.getMasterBranch(), initOptions.getTestBranch()));
                        } else {
                            NotifyUtil.notifyError(myProject, "Error", String.format("MrtfGitFlow初始化失败：%s", result.getErrorOutputAsJoinedString()));
                            return;
                        }
                    }

                    // 校验主发布支是否存在，不存在就新建
                    if (!remoteBranches.contains(initOptions.getReleaseBranch())) {
                        ErrorsListener errorListener = new ErrorsListener(project);
                        // 新建分支发布分支
                        GitCommandResult result = mrtfGitFlow.newNewBranchBaseRemoteMaster(repository, initOptions.getMasterBranch(), initOptions.getReleaseBranch(), errorListener);
                        if (result.success()) {
                            NotifyUtil.notifySuccess(myProject, "Success", String.format("基于 origin/%s 成功创建分支 %s ", initOptions.getMasterBranch(), initOptions.getReleaseBranch()));
                        } else {
                            NotifyUtil.notifyError(myProject, "Error", String.format("MrtfGitFlow初始化失败：%s", result.getErrorOutputAsJoinedString()));
                            return;
                        }
                    }

                    // 存储配置
                    String configJson = JSON.toJSONString(initOptions);
                    ConfigUtil.saveConfigToLocal(project, configJson);
                    ConfigUtil.saveConfigToFile(project, configJson);

                    // 将配置文件加入GIT管理
                    mrtfGitFlow.addConfigToGit(repository);

                    NotifyUtil.notifySuccess(myProject, "Success", "MrtfGitFlow初始化成功");

                    //update the widget
                    myProject.getMessageBus().syncPublisher(GitRepository.GIT_REPO_CHANGE).repositoryChanged(repository);
                    repository.update();
                    VirtualFileManager.getInstance().asyncRefresh(null);
                }
            }.queue();
        }
    }

}
