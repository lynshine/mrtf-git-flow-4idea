package com.github.xiaolyuh.ui;

import com.github.xiaolyuh.GitFlowPlus;
import com.github.xiaolyuh.TagOptions;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.github.xiaolyuh.utils.StringUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.regex.Pattern;

/**
 * @author yuhao.wang3
 * @since 2020/3/27 12:15
 */
public class TagDialog extends DialogWrapper {
    private JPanel tagPanel;

    private JTextField tagNameTextField;
    private JTextArea messageTextArea;

    private Project project;

    public TagDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("请输入Tag信息");
        init();
    }

    public TagOptions getTagOptions() {
        TagOptions tagOptions = new TagOptions();
        tagOptions.setTagName(StringUtils.trim(tagNameTextField.getText()));
        tagOptions.setMessage(StringUtils.trim(messageTextArea.getText()));
        return tagOptions;
    }

    @javax.annotation.Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (StringUtils.isBlank(tagNameTextField.getText())) {
            return new ValidationInfo("Tag名称不能为空", tagNameTextField);
        }
        boolean isMatch = Pattern.matches("^[A-Za-z0-9\\-._\\u4e00-\\u9fa5]+$", tagNameTextField.getText());
        if (!isMatch) {
            return new ValidationInfo("Tag名称不合法，支持汉字、字母、数字、-、_和.", tagNameTextField);
        }
        final GitRepository repository = GitBranchUtil.getCurrentRepository(project);
        if (GitFlowPlus.getInstance().isExistTag(repository, tagNameTextField.getText())) {
            return new ValidationInfo("Tag名称已经存在", tagNameTextField);
        }
        if (StringUtils.isBlank(messageTextArea.getText())) {
            return new ValidationInfo("Tag备注不能为空", messageTextArea);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return tagPanel;
    }
}
