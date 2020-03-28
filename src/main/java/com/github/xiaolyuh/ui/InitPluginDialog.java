package com.github.xiaolyuh.ui;

import com.github.xiaolyuh.InitOptions;
import com.github.xiaolyuh.utils.ConfigUtil;
import com.github.xiaolyuh.utils.GitBranchUtil;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CollectionComboBoxModel;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 初始化插件弹框
 *
 * @author yuhao.wang3
 */
public class InitPluginDialog extends DialogWrapper {
    private JPanel contentPane;

    private JComboBox<String> masterBranchComboBox;
    private JComboBox<String> releaseBranchComboBox;
    private JComboBox<String> testBranchComboBox;
    private JTextField featurePrefixTextField;
    private JTextField hotfixPrefixTextField;
    private JTextField tagPrefixTextField;
    private JTextField dingtalkTokenTextField;
    private JCheckBox releaseFinishIsDeleteReleaseCheckBox;
    private JCheckBox releaseFinishIsDeleteFeatureCheckBox;

    public InitPluginDialog(Project project) {
        super(project);

        setTitle("插件初始化");

        initDialog(project);

        enableFields();

        init();
    }

    private void enableFields() {
        masterBranchComboBox.setEnabled(true);
        releaseBranchComboBox.setEnabled(true);
        testBranchComboBox.setEnabled(true);
        featurePrefixTextField.setEnabled(true);
        hotfixPrefixTextField.setEnabled(true);
        tagPrefixTextField.setEnabled(true);
        releaseFinishIsDeleteReleaseCheckBox.setEnabled(false);
        releaseFinishIsDeleteFeatureCheckBox.setEnabled(false);
    }


    public InitOptions getOptions() {
        InitOptions options = new InitOptions();

        options.setMasterBranch((String) masterBranchComboBox.getSelectedItem());
        options.setReleaseBranch((String) releaseBranchComboBox.getSelectedItem());
        options.setTestBranch((String) testBranchComboBox.getSelectedItem());
        options.setFeaturePrefix(featurePrefixTextField.getText());
        options.setHotfixPrefix(hotfixPrefixTextField.getText());
        options.setTagPrefix(tagPrefixTextField.getText());
        options.setReleaseFinishIsDeleteFeature(releaseFinishIsDeleteFeatureCheckBox.isSelected());
        options.setReleaseFinishIsDeleteRelease(releaseFinishIsDeleteReleaseCheckBox.isSelected());
        options.setDingtalkToken(dingtalkTokenTextField.getText());

        return options;
    }

    /**
     * 初始化弹框
     */
    private void initDialog(Project project) {
        Optional<InitOptions> options = ConfigUtil.getConfig(project);
        List<String> remoteBranches = GitBranchUtil.getRemoteBranches(project);
        if (options.isPresent()) {
            List<String> masterBranchList = Lists.newArrayList(options.get().getMasterBranch());
            List<String> releaseBranchList = Lists.newArrayList(options.get().getReleaseBranch());
            List<String> testBranchList = Lists.newArrayList(options.get().getTestBranch());
            masterBranchList.addAll(remoteBranches);
            releaseBranchList.addAll(remoteBranches);
            testBranchList.addAll(remoteBranches);
            masterBranchComboBox.setModel(new CollectionComboBoxModel<>(masterBranchList));
            releaseBranchComboBox.setModel(new CollectionComboBoxModel<>(releaseBranchList));
            testBranchComboBox.setModel(new CollectionComboBoxModel<>(testBranchList));

            featurePrefixTextField.setText(options.get().getFeaturePrefix());
            hotfixPrefixTextField.setText(options.get().getHotfixPrefix());
            tagPrefixTextField.setText(options.get().getTagPrefix());
            releaseFinishIsDeleteReleaseCheckBox.setSelected(false);
            releaseFinishIsDeleteFeatureCheckBox.setSelected(false);
            dingtalkTokenTextField.setText(options.get().getDingtalkToken());
        } else {

            masterBranchComboBox.setModel(new CollectionComboBoxModel<>(remoteBranches));
            releaseBranchComboBox.setModel(new CollectionComboBoxModel<>(remoteBranches));
            testBranchComboBox.setModel(new CollectionComboBoxModel<>(remoteBranches));
        }
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
    protected ValidationInfo doValidate() {
        if (Objects.equals(masterBranchComboBox.getSelectedItem(), releaseBranchComboBox.getSelectedItem())) {
            return new ValidationInfo("发布分支和主干分支不能相同", releaseBranchComboBox);
        }
        if (Objects.equals(masterBranchComboBox.getSelectedItem(), testBranchComboBox.getSelectedItem())) {
            return new ValidationInfo("测试分支和主干分支不能相同", testBranchComboBox);
        }
        if (releaseBranchComboBox.getSelectedItem().equals(testBranchComboBox.getSelectedItem())) {
            return new ValidationInfo("测试分支和发布分支不能相同", testBranchComboBox);
        }
        if (StringUtil.isEmptyOrSpaces(featurePrefixTextField.getText())) {
            return new ValidationInfo("请填写开发分支的前缀", featurePrefixTextField);
        }
        if (StringUtil.isEmptyOrSpaces(hotfixPrefixTextField.getText())) {
            return new ValidationInfo("请填写修复分支的前缀", hotfixPrefixTextField);
        }

        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
