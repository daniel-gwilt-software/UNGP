package data.scripts.campaign.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_Settings;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.campaign.inherit.UNGP_InheritManager;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.intel.UNGP_ChallengeIntel;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import data.scripts.campaign.specialist.rules.UNGP_RulePickListener;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import data.scripts.ungpsaves.UNGP_DataSaverAPI;
import data.scripts.ungpsaves.impl.UNGP_BlueprintsDataSaver;
import data.scripts.utils.UNGP_Feedback;
import org.lwjgl.input.Keyboard;
import ungp.ui.CheckBoxGroup;
import ungp.ui.HorizontalButtonGroup;
import ungp.ui.SettingEntry;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static data.scripts.campaign.UNGP_Settings.d_i18n;
import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.rulesMeetCondition;

public class UNGP_InteractionDialog implements InteractionDialogPlugin {
    private enum OptionID {
        CHECK_INHERIT_SLOTS,
        CHECK_RECORD_SLOTS,
        HELP,

        CHOOSE_INHERIT_SLOT_0,
        CHOOSE_INHERIT_SLOT_1,
        CHOOSE_INHERIT_SLOT_2,
        PICK_RULES,
        INHERIT,
        INHERIT_SETTINGS,

        //        START_RECORD,
        CHOOSE_RECORD_SLOT_0,
        CHOOSE_RECORD_SLOT_1,
        CHOOSE_RECORD_SLOT_2,

        BACK_TO_MENU,
        LEAVE
    }

    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private SectorAPI sector;
    private UNGP_InteractionPanelPlugin uiPanelPlugin;

    private UNGP_InGameData inGameData;

    private UNGP_InheritData pickedInheritData;
    private OptionID choseInheritSlotOptionID = null;

    private UNGP_InheritData pregenInheritData;
    private boolean isSpecialistMode = false;

    // inherit setting entry
    private SettingEntry<Float> setting_inheritCreditsRatio = new SettingEntry<>(0f);
    private SettingEntry<Float> setting_inheritBPsRatio = new SettingEntry<>(0f);
    private SettingEntry<Difficulty> setting_difficulty = new SettingEntry<>(null);

    private List<URule> pickedRules = new ArrayList<>();


    public UNGP_InteractionDialog(UNGP_InGameData inGameData) {
        this.inGameData = inGameData;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        dialog.setPromptText("");
        dialog.setBackgroundDimAmount(0.4f);
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        sector = Global.getSector();

        pregenInheritData = UNGP_InheritData.createInheritData(inGameData);
        UNGP_InheritManager.loadAllSlots();

//        TooltipMakerAPI fakeTooltip = textPanel.beginTooltip();
//        textPanel.addTooltip();

        uiPanelPlugin = new UNGP_InteractionPanelPlugin();
        uiPanelPlugin.update(visual);
        initMenu();
        dialog.setOptionOnEscape(null, OptionID.LEAVE);
    }

    /**
     * The beginning of the page.
     */
    private void initMenu() {
        textPanel.addPara(d_i18n.get("menu"));
        options.addOption(d_i18n.get("checkInherit"), OptionID.CHECK_INHERIT_SLOTS);
        options.addOption(d_i18n.get("checkRecord"), OptionID.CHECK_RECORD_SLOTS);
        if (!inGameData.isInherited() && inGameData.isPassedInheritTime()) {
            textPanel.addPara(d_i18n.get("hasPassedTime"), Misc.getNegativeHighlightColor());
        }
        if (!inGameData.couldStartRecord()) {
            options.setEnabled(OptionID.CHECK_RECORD_SLOTS, false);
            if (inGameData.isRecorded()) {
                textPanel.addPara(d_i18n.get("hasRecorded"), Misc.getNegativeHighlightColor());
            }
            if (!UNGP_Settings.reachMaxLevel()) {
                textPanel.addPara(d_i18n.get("notMaxLevel"), Misc.getNegativeHighlightColor());
            }
        }
        TooltipMakerAPI toRecordInfo = textPanel.beginTooltip();
        Difficulty difficulty = null;
        if (inGameData.isHardMode()) {
            difficulty = inGameData.getDifficulty();
        }
        pregenInheritData.addRecordTooltip(toRecordInfo, difficulty);
        textPanel.addTooltip();
        options.addOption(d_i18n.get("help"), OptionID.HELP);
        addLeaveButton();
    }

    /**
     * 选择继承槽位
     */
    private void optionSelectedChooseInherit(OptionID option) {
        int slotID = 0;
        switch (option) {
            case CHOOSE_INHERIT_SLOT_0:
                break;
            case CHOOSE_INHERIT_SLOT_1:
                slotID = 1;
                break;
            case CHOOSE_INHERIT_SLOT_2:
                slotID = 2;
                break;
        }
        pickedInheritData = UNGP_InheritManager.getDataFromSlot(slotID);
        Color nC = Misc.getNegativeHighlightColor();
        if (pickedInheritData != null) {
            choseInheritSlotOptionID = option;
            TooltipMakerAPI inheritDataInfo = textPanel.beginTooltip();
            pickedInheritData.addInheritTooltip(inheritDataInfo);
            textPanel.addTooltip();

            //如果没有继承过或者没有超过时限
            if (!inGameData.isPassedInheritTime() && !inGameData.isInherited()) {
                // 继承选项
                String settingOptionStr = d_i18n.get("startSetting");
                options.addOption(settingOptionStr, OptionID.INHERIT_SETTINGS);
                options.setShortcut(OptionID.INHERIT_SETTINGS, Keyboard.KEY_S, false, false, false, true);

                if (isSpecialistMode) {
                    options.addOption(d_i18n.get("rulepick_button") + (UNGP_ChallengeManager.isDifficultyEnough(setting_difficulty.get()) ?
                                                                       d_i18n.get("rulepick_couldChallenge") : ""), OptionID.PICK_RULES);
                    options.setShortcut(OptionID.PICK_RULES, Keyboard.KEY_R, false, false, false, true);
                    pickedRules.clear();
                }

                options.addOption(d_i18n.get("startInherit"), OptionID.INHERIT);
                options.setShortcut(OptionID.INHERIT, Keyboard.KEY_SPACE, false, false, false, true);
                updateOptionsFromSettings();
            } else {
                if (inGameData.isInherited()) {
                    textPanel.addPara(d_i18n.get("hasInherited"), nC);
                } else {
                    textPanel.addPara(d_i18n.get("hasPassedTime"), nC);
                }
            }
        } else {
            textPanel.addPara(d_i18n.get("noInherit"), nC);
        }
    }

    private void saveRecordByChosenOption(UNGP_InheritData dataToRecord, OptionID option) {
        int slotID = 0;
        switch (option) {
            case CHOOSE_RECORD_SLOT_0:
                break;
            case CHOOSE_RECORD_SLOT_1:
                slotID = 1;
                break;
            case CHOOSE_RECORD_SLOT_2:
                slotID = 2;
                break;
        }
        UNGP_InheritManager.saveDataToSlot(dataToRecord, slotID);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        final OptionID selectedOption = (OptionID) optionData;
        if (selectedOption != OptionID.PICK_RULES) {
            options.clearOptions();
            textPanel.clear();
            uiPanelPlugin.update(visual);
        }
        switch (selectedOption) {
            case CHECK_INHERIT_SLOTS: {
                textPanel.addPara(d_i18n.get("checkInherit"));
                textPanel.addPara(d_i18n.get("checkInheritSlot"));
                UNGP_InheritData curSlot = UNGP_InheritManager.InheritData_slot0;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_0);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_0, false);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_0);
                }
                curSlot = UNGP_InheritManager.InheritData_slot1;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_1);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_1, false);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_1);
                }
                curSlot = UNGP_InheritManager.InheritData_slot2;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_INHERIT_SLOT_2);
                    options.setEnabled(OptionID.CHOOSE_INHERIT_SLOT_2, false);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_INHERIT_SLOT_2);
                }
                resetSettings();
                addBackButton(OptionID.BACK_TO_MENU);
            }
            break;
            case CHOOSE_INHERIT_SLOT_0:
            case CHOOSE_INHERIT_SLOT_1:
            case CHOOSE_INHERIT_SLOT_2:
                optionSelectedChooseInherit(selectedOption);
                addBackButton(OptionID.CHECK_INHERIT_SLOTS);
                break;
            case CHECK_RECORD_SLOTS: {
                TooltipMakerAPI recordInfo = textPanel.beginTooltip();
                textPanel.addPara(d_i18n.get("recordInfo"));
                pregenInheritData.addRecordTooltip(recordInfo, inGameData.getDifficulty());
                textPanel.addTooltip();
                textPanel.addPara(d_i18n.get("checkRecordSlot"));
                // 三个重生槽位
                UNGP_InheritData curSlot = UNGP_InheritManager.InheritData_slot0;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_0);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_0);
                }
                curSlot = UNGP_InheritManager.InheritData_slot1;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_1);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_1);
                }
                curSlot = UNGP_InheritManager.InheritData_slot2;
                if (curSlot == null) {
                    options.addOption(d_i18n.get("emptySlot"), OptionID.CHOOSE_RECORD_SLOT_2);
                } else {
                    int curCycle = Math.max(0, curSlot.cycle - 1);
                    options.addOption(d_i18n.format("slotDes", curCycle + "", curSlot.lastPlayerName)
                            , OptionID.CHOOSE_RECORD_SLOT_2);
                }
                addBackButton(OptionID.BACK_TO_MENU);
            }
            break;
            case HELP:
                textPanel.addPara(d_i18n.get("helpInfo"));
                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case PICK_RULES:
                final List<URule> oldList = new ArrayList<>(pickedRules);
                pickedRules.clear();
                final Difficulty difficulty = setting_difficulty.get();
                UNGP_RulesManager.setStaticDifficulty(difficulty);
                UNGP_RulePickListener pickListener = new UNGP_RulePickListener(pickedRules,
                                                                               pickedInheritData.completedChallenges,
                                                                               difficulty, new Script() {
                    @Override
                    public void run() {
                        setSpecialistModeToolTip();
                        uiPanelPlugin.update(visual);
                        TooltipMakerAPI tooltip = uiPanelPlugin.beginTooltip(0f, false);
                        tooltip.addPara(d_i18n.get("hardmodeDes"), Misc.getHighlightColor(), 0f);
                        uiPanelPlugin.addTooltip(20f, tooltip);
                        tooltip = uiPanelPlugin.beginTooltip(300f, true);
//                        TooltipMakerAPI tooltip = textPanel.beginTooltip();
                        for (URule rule : pickedRules) {
                            TooltipMakerAPI imageMaker = tooltip.beginImageWithText(rule.getSpritePath(), 32f);
                            imageMaker.addPara(rule.getName(), rule.getCorrectColor(), 0f);
                            rule.addDesc(imageMaker, 0f);
                            tooltip.addImageWithText(3f);
                        }
                        uiPanelPlugin.addTooltip(300f, tooltip);
                        tooltip = uiPanelPlugin.beginTooltip(10f, false);
                        // 如果满足规则
                        if (!rulesMeetCondition(pickedRules, difficulty)) {
                            tooltip.setParaOrbitronLarge();
                            tooltip.addPara(d_i18n.get("rulepick_notMeet"), Misc.getNegativeHighlightColor(), 5f);
                            tooltip.setParaFontDefault();
                            uiPanelPlugin.addTooltip(10f, tooltip);

                        } else {
                            List<UNGP_ChallengeInfo> runnableChallenges = UNGP_ChallengeManager.getRunnableChallenges(difficulty, pickedRules, pickedInheritData.completedChallenges);
                            if (!runnableChallenges.isEmpty()) {
                                tooltip.addPara(d_i18n.get("rulepick_runnableChallenges"), Misc.getHighlightColor(), 10f);
                                uiPanelPlugin.addTooltip(20f, tooltip);
                                tooltip = uiPanelPlugin.beginTooltip(300f, true);
                                for (UNGP_ChallengeInfo challenge : runnableChallenges) {
                                    challenge.createTooltip(tooltip, 5f, 0);
                                }
                                uiPanelPlugin.addTooltip(300f, tooltip);
                            }
                        }
//                        textPanel.addTooltip();
                    }
                }, new Script() {
                    @Override
                    public void run() {
                        pickedRules.addAll(oldList);
                    }
                });
                pickListener.showCargoPickerDialog(dialog);
                break;
            case INHERIT:
                inherit();
                addLeaveButton();
                break;
            case CHOOSE_RECORD_SLOT_0:
            case CHOOSE_RECORD_SLOT_1:
            case CHOOSE_RECORD_SLOT_2:
                dialog.showCustomDialog(720f, 160f, new RecordDialogDelegate(selectedOption));
                addLeaveButton();
                break;
            case BACK_TO_MENU:
                initMenu();
                break;
            case LEAVE:
                UNGP_InheritManager.clearSlots();
                dialog.dismiss();
                break;
            case INHERIT_SETTINGS:
                dialog.showCustomDialog(720f, 300f, new InheritOptionsDelegate());
                break;
            default:
                break;
        }

    }

    /**
     * 继承重生点
     */
    private void inherit() {
        int creditsInherited = (int) (pickedInheritData.inheritCredits * setting_inheritCreditsRatio.get());
        sector.getPlayerFleet().getCargo().getCredits().add(creditsInherited);
        AddRemoveCommodity.addCreditsGainText(creditsInherited, textPanel);
        float inheritBPPercent = setting_inheritBPsRatio.get();
        Map<String, Object> dataSaverParams = new HashMap<>();
        dataSaverParams.put("inheritBPPercent", inheritBPPercent);
        for (UNGP_DataSaverAPI dataSaver : pickedInheritData.dataSavers) {
            TooltipMakerAPI tooltip = textPanel.beginTooltip();
            dataSaver.startInheritDataFromSaver(tooltip, dataSaverParams);
            textPanel.addTooltip();
        }
        // Add points: skill points/story points
        int addSkillPoints = UNGP_Settings.getBonusSkillPoints(pickedInheritData.cycle);
        int addStoryPoints = UNGP_Settings.getBonusStoryPoints(pickedInheritData.cycle);
        textPanel.addPara(d_i18n.get("inheritedPoints"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                          addSkillPoints + "");
        sector.getPlayerStats().addPoints(addSkillPoints);
        sector.getPlayerStats().addStoryPoints(addStoryPoints, textPanel, true);


        textPanel.setFontInsignia();

        if (isSpecialistMode)
            textPanel.addPara(d_i18n.get("hardModeYes"), Misc.getNegativeHighlightColor());

        inGameData.setCurCycle(pickedInheritData.cycle);
        inGameData.setInherited(true);
        inGameData.setHardMode(isSpecialistMode);
        inGameData.setCompletedChallenges(pickedInheritData.completedChallenges);
        if (isSpecialistMode) {
            inGameData.setDifficulty(setting_difficulty.get());
            inGameData.saveActivatedRules(pickedRules);
            UNGP_Feedback.setFeedBackList(pickedRules);
            UNGP_SpecialistIntel intel = new UNGP_SpecialistIntel();
            Global.getSector().getIntelManager().addIntel(intel, false, textPanel);
            UNGP_ChallengeIntel challengeIntel = UNGP_ChallengeManager.confirmChallenges(inGameData);
            if (challengeIntel != null) {
                Global.getSector().getIntelManager().addIntelToTextPanel(challengeIntel, textPanel);
            }
            UNGP_RulesManager.updateRulesCache();
        }
    }

    /**
     * 记录重生点
     *
     * @param option
     */
    private void record(OptionID option) {
        inGameData.setRecorded(true);
        saveRecordByChosenOption(pregenInheritData, option);
        Global.getSoundPlayer().playUISound("ui_rep_raise", 1, 1);
        textPanel.addPara(d_i18n.get("recordSuccess"));
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    private void setSpecialistModeToolTip() {
        if (options.hasOption(OptionID.INHERIT)) {
            if (!pickedRules.isEmpty()) {
                String[] ruleNames = new String[pickedRules.size()];
                Color[] ruleColors = new Color[pickedRules.size()];
                StringBuilder result = new StringBuilder(d_i18n.get("hardmodeDes"));
                for (int i = 0; i < pickedRules.size(); i++) {
                    URule rule = pickedRules.get(i);
                    result.append("\n  ");
                    result.append(rule.getName());
                    ruleNames[i] = rule.getName();
                    ruleColors[i] = rule.getCorrectColor();
                }
                options.setTooltip(OptionID.INHERIT, result.toString());
                options.setTooltipHighlights(OptionID.INHERIT, ruleNames);
                options.setTooltipHighlightColors(OptionID.INHERIT, ruleColors);
            }
        }
    }

    @Override
    public void advance(float amount) {
        if (isSpecialistMode && options.hasOption(OptionID.INHERIT)) {
            options.setEnabled(OptionID.INHERIT, rulesMeetCondition(pickedRules, setting_difficulty.get()));
        }
    }

    /**
     * 继承
     */
    private void updateOptionsFromSettings() {
        if (options.hasOption(OptionID.INHERIT)) {
            final int creditsInherited = (int) (pickedInheritData.inheritCredits * setting_inheritCreditsRatio.get());
            int bpInheritGeneratedByDataSaver = 0;
            for (UNGP_DataSaverAPI dataSaver : pickedInheritData.dataSavers) {
                if (dataSaver instanceof UNGP_BlueprintsDataSaver) {
                    UNGP_BlueprintsDataSaver blueprintsDataSaver = (UNGP_BlueprintsDataSaver) dataSaver;
                    bpInheritGeneratedByDataSaver = (int) ((blueprintsDataSaver.ships.size() +
                            blueprintsDataSaver.fighters.size() +
                            blueprintsDataSaver.weapons.size() +
                            blueprintsDataSaver.hullmods.size())
                            * setting_inheritBPsRatio.get());
                }
            }
            final int bpInherited = bpInheritGeneratedByDataSaver;
            final int addSkillPoints = UNGP_Settings.getBonusSkillPoints(pickedInheritData.cycle);
            final int addStoryPoints = UNGP_Settings.getBonusStoryPoints(pickedInheritData.cycle);
            final boolean isSpecialistMode = this.isSpecialistMode;
            final Difficulty difficulty = setting_difficulty.get();

            TooltipMakerAPI tooltip = textPanel.beginTooltip();
            TooltipMakerAPI section = tooltip.beginImageWithText("graphics/icons/reports/storage24.png", 24f);
            section.addPara(d_i18n.get("inheritOptions"), Misc.getBasePlayerColor(), 0f);
            tooltip.addImageWithText(5f);
            tooltip.setBulletedListMode("       ");
            tooltip.addPara(d_i18n.get("inheritCredits") + ": %s", 5f, Misc.getHighlightColor(), (int) (setting_inheritCreditsRatio.get() * 100f) + "%");
            tooltip.addPara(d_i18n.get("inheritBPs") + ": %s", 5f, Misc.getPositiveHighlightColor(), (int) (setting_inheritBPsRatio.get() * 100f) + "%");
            if (difficulty != null) {
                tooltip.addPara(d_i18n.get("hardmodeLevel") + ": %s", 5f, difficulty.color, difficulty.name);
            }
            tooltip.setBulletedListMode(null);
            textPanel.addTooltip();
            // 确认
            options.addOptionConfirmation(OptionID.INHERIT, new CustomStoryDialogDelegate() {
                @Override
                public String getTitle() {
                    return d_i18n.get("startInherit");
                }

                @Override
                public void createDescription(TooltipMakerAPI info) {
                    float pad = 10f;
                    String credits = Misc.getDGSCredits(creditsInherited);
                    Color hl = Misc.getHighlightColor();
                    Color negative = Misc.getNegativeHighlightColor();
                    info.addPara(d_i18n.get("inheritConfirmInfo0"), 0f, hl, credits, "" + bpInherited);
                    info.addSpacer(pad);
                    info.addPara(d_i18n.get("inheritConfirmTip_p1"), 0f, Misc.getBasePlayerColor(), hl, addSkillPoints + "");
                    info.addPara(d_i18n.get("inheritConfirmTip_p2"), 0f, Misc.getStoryOptionColor(), hl, addStoryPoints + "");
                    if (difficulty != null && isSpecialistMode) {
                        info.addPara(d_i18n.get("inheritConfirmInfo1"), negative, 0f);
                        info.addSectionHeading(d_i18n.format("rulepick_level", difficulty.name), hl, Misc.scaleAlpha(negative, 0.2f), Alignment.MID, pad * 0.5f);
                        float width = info.getPrev().getPosition().getWidth();
                        int ruleSize = pickedRules.size();
                        int itemsPerRow = (int) (width / 64f);
                        int page = Math.max(0, ruleSize - 1) / itemsPerRow;

                        for (int i = 0; i <= page; i++) {
                            List<String> ruleSprites = new ArrayList<>();
                            for (int j = i * itemsPerRow; j < (i + 1) * itemsPerRow; j++) {
                                if (j < ruleSize) {
                                    ruleSprites.add(pickedRules.get(j).getSpritePath());
                                }
                            }
                            if (!ruleSprites.isEmpty()) {
                                String[] array = ruleSprites.toArray(new String[0]);
                                info.addImages(width, 64f, 0f, 4f, array);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    private void addLeaveButton() {
        options.addOption(d_i18n.get("leave"), OptionID.LEAVE);
    }

    private void addBackButton(OptionID warpOption) {
        options.addOption(d_i18n.get("back"), warpOption);
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }

    public void resetSettings() {
        setting_difficulty.reset();
        setting_inheritBPsRatio.reset();
        setting_inheritCreditsRatio.reset();
        isSpecialistMode = false;
        pickedRules.clear();
    }

    private class InheritOptionsDelegate implements CustomDialogDelegate {

        CheckBoxGroup creditsGroup = new CheckBoxGroup();
        CheckBoxGroup bpsGroup = new CheckBoxGroup();
        CheckBoxGroup difficultyGroup = new CheckBoxGroup();

        @Override
        public void createCustomDialog(CustomPanelAPI panel) {
            creditsGroup.clear();
            bpsGroup.clear();
            difficultyGroup.clear();
            float width = panel.getPosition().getWidth();
            float height = panel.getPosition().getHeight();
            float pad = 5f;

            TooltipMakerAPI tooltip = panel.createUIElement(width, height, true);
            panel.addUIElement(tooltip);

//            tooltip.setForceProcessInput(true);
            tooltip.setParaOrbitronLarge();
            tooltip.setAreaCheckboxFont(Fonts.ORBITRON_24AA);
            tooltip.addPara(d_i18n.get("inheritCredits"), Misc.getHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            float buttonHeight = 30f;
            final float buttonWidth = width / pad - 10f;
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                for (int i = 0; i < 5; i++) {
                    float percentage = Math.min(1, i * 0.25f);
                    ButtonAPI checkBox = tooltip.addAreaCheckbox((int) (percentage * 100f) + "%", null, Misc.getBasePlayerColor(),
                                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                 buttonWidth, buttonHeight, 0f);
                    buttonGroup.addButton(checkBox);
                    creditsGroup.addCheckBox(checkBox, percentage);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            tooltip.addSpacer(pad);
            tooltip.addPara(d_i18n.get("inheritBPs"), Misc.getPositiveHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                for (int i = 0; i < 5; i++) {
                    float percentage = Math.min(1, i * 0.25f);
                    ButtonAPI checkBox = tooltip.addAreaCheckbox((int) (percentage * 100f) + "%", null, Misc.getBasePlayerColor(),
                                                                 Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                                 buttonWidth, buttonHeight, 0f);
                    buttonGroup.addButton(checkBox);
                    bpsGroup.addCheckBox(checkBox, percentage);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            tooltip.addSpacer(40f);
            tooltip.addPara(d_i18n.get("hardmodeLevel"), Misc.getNegativeHighlightColor(), 0f);
            tooltip.addSpacer(pad);
            {
                HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
                Difficulty[] difficulties = Difficulty.values();
                for (int i = 0; i < difficulties.length + 1; i++) {
                    ButtonAPI checkBox;
                    Difficulty difficulty;
                    if (i == 0) {
                        difficulty = null;
                        checkBox = tooltip.addAreaCheckbox("/", null, Misc.getBasePlayerColor(),
                                                           Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
                                                           buttonWidth, buttonHeight, 0f);
                    } else {
                        difficulty = difficulties[i - 1];
                        checkBox = tooltip.addAreaCheckbox(difficulty.name, null, difficulty.color,
                                                           difficulty.color.darker(), difficulty.color.brighter(),
                                                           buttonWidth, buttonHeight, 0f);
                    }
                    tooltip.addTooltipToPrevious(new DifficultyTooltipCreator(difficulty), TooltipMakerAPI.TooltipLocation.BELOW);
                    buttonGroup.addButton(checkBox);
                    difficultyGroup.addCheckBox(checkBox, difficulty);
                }
                buttonGroup.updateTooltip(tooltip, pad);
            }
            creditsGroup.tryCheckValue(setting_inheritCreditsRatio.get());
            bpsGroup.tryCheckValue(setting_inheritBPsRatio.get());
            difficultyGroup.tryCheckValue(setting_difficulty.get());
        }

        private class DifficultyTooltipCreator implements TooltipMakerAPI.TooltipCreator {
            private Difficulty difficulty;

            public DifficultyTooltipCreator(Difficulty difficulty) {
                this.difficulty = difficulty;
            }

            @Override
            public boolean isTooltipExpandable(Object tooltipParam) {
                return false;
            }

            @Override
            public float getTooltipWidth(Object tooltipParam) {
                return 200f;
            }

            @Override
            public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                if (difficulty == null) {
                    tooltip.addPara(d_i18n.get("difficulty_desc_null"), 0);
                } else {
                    Color hl = Misc.getHighlightColor();
                    tooltip.addPara(d_i18n.get("difficulty_desc_base") + "%s / %s", 0f, hl,
                                    difficulty.minRules + "",
                                    difficulty.maxRules + "");
                    tooltip.addPara(d_i18n.get("difficulty_desc_value") + "%s", 0f, hl,
                                    (int) (difficulty.extraValueMultiplier * 100f) + "%");
                    if (UNGP_ChallengeManager.isDifficultyEnough(difficulty)) {
                        tooltip.addPara(d_i18n.get("difficulty_desc_max"), hl, 5f);
                    }
                }
            }
        }

        @Override
        public boolean hasCancelButton() {
            return true;
        }

        @Override
        public String getConfirmText() {
            return null;
        }

        @Override
        public String getCancelText() {
            return null;
        }

        @Override
        public void customDialogConfirm() {
            setting_inheritCreditsRatio.set((Float) creditsGroup.getCheckedValue());
            setting_inheritBPsRatio.set((Float) bpsGroup.getCheckedValue());
            Difficulty difficulty = (Difficulty) difficultyGroup.getCheckedValue();
            setting_difficulty.set(difficulty);
            isSpecialistMode = difficulty != null;
            optionSelected(null, choseInheritSlotOptionID);
        }

        @Override
        public void customDialogCancel() {
            optionSelected(null, choseInheritSlotOptionID);
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return new CustomUIPanelPlugin() {
                @Override
                public void positionChanged(PositionAPI position) {

                }

                @Override
                public void renderBelow(float alphaMult) {

                }

                @Override
                public void render(float alphaMult) {

                }

                @Override
                public void advance(float amount) {
                    creditsGroup.updateCheck();
                    bpsGroup.updateCheck();
                    difficultyGroup.updateCheck();
                }

                @Override
                public void processInput(List<InputEventAPI> events) {

                }
            };
        }
    }

    private class CustomStoryDialogDelegate extends BaseStoryPointActionDelegate {
        @Override
        public boolean withDescription() {
            return true;
        }

        @Override
        public boolean withSPInfo() {
            return false;
        }

        @Override
        public String getLogText() {
            return null;
        }

        @Override
        public float getBonusXPFraction() {
            return 0;
        }

        @Override
        public TextPanelAPI getTextPanel() {
            if (dialog == null) return null;
            return textPanel;
        }

        @Override
        public String getConfirmSoundId() {
            return "ui_acquired_blueprint";
        }

        @Override
        public int getRequiredStoryPoints() {
            return 0;
        }
    }

    /**
     * Used while saving.
     */
    private class RecordDialogDelegate implements CustomDialogDelegate {
        private ButtonAPI btn_recordCargo;
        private ButtonAPI btn_recordShip;
        private ButtonAPI btn_recordColony;

        private OptionID selectedOption;

        public RecordDialogDelegate(OptionID selectedOption) {
            this.selectedOption = selectedOption;
        }

        @Override
        public void createCustomDialog(CustomPanelAPI panel) {
            float width = panel.getPosition().getWidth();
            float height = panel.getPosition().getHeight();
            float pad = 5f;

            TooltipMakerAPI info = panel.createUIElement(width, height, true);
            panel.addUIElement(info);
            info.setParaOrbitronLarge();
            info.addPara(d_i18n.get("recordConfirmInfo"), Misc.getNegativeHighlightColor(), 0f);
            info.addSpacer(30f);
            info.setAreaCheckboxFont(Fonts.ORBITRON_24AA);
            info.addPara(d_i18n.get("recordExtraCreditsTitle"), Misc.getHighlightColor(), 0f);
            info.addSpacer(pad);
            float buttonWidth = width / 3f - 10f;
            float buttonHeight = 30f;
            HorizontalButtonGroup buttonGroup = new HorizontalButtonGroup();
            btn_recordCargo = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_cargo"), null, Misc.getBasePlayerColor(),
                                                   Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            btn_recordShip = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_ship"), null, Misc.getBasePlayerColor(),
                                                  Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            btn_recordColony = info.addAreaCheckbox(d_i18n.get("recordExtraCredits_colony"), null, Misc.getBasePlayerColor(),
                                                    Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(), buttonWidth, buttonHeight, 0f);
            buttonGroup.addButton(btn_recordCargo);
            buttonGroup.addButton(btn_recordShip);
            buttonGroup.addButton(btn_recordColony);

            buttonGroup.updateTooltip(info, 10f);
        }

        @Override
        public boolean hasCancelButton() {
            return true;
        }

        @Override
        public String getConfirmText() {
            return null;
        }

        @Override
        public String getCancelText() {
            return null;
        }

        @Override
        public void customDialogConfirm() {
            float extraCredits = 0;
            boolean recordCargo = btn_recordCargo.isChecked();
            boolean recordShip = btn_recordShip.isChecked();
            boolean recordIndustry = btn_recordColony.isChecked();
            CargoAPI convertCargo = Global.getFactory().createCargo(true);
            for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
                if (Misc.playerHasStorageAccess(market)) {
                    CargoAPI storageCargo = Misc.getStorageCargo(market);
                    if (storageCargo != null) {
                        if (recordCargo) {
                            convertCargo.addAll(storageCargo);
                        }
                        if (recordShip) {
                            FleetDataAPI mothballedShips = storageCargo.getMothballedShips();
                            if (mothballedShips != null)
                                for (FleetMemberAPI member : mothballedShips.getMembersListCopy()) {
                                    extraCredits += member.getBaseValue();
                                }
                        }
                    }
                }
                if (recordIndustry) {
                    if (market.isPlayerOwned()) {
                        for (Industry industry : market.getIndustries()) {
                            extraCredits += industry.getBuildCost();
                        }
                    }
                }
            }
            CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
            CargoAPI playerCargo = playerFleet.getCargo();
            if (recordCargo) {
                convertCargo.addAll(playerCargo);
                for (CargoStackAPI stack : convertCargo.getStacksCopy()) {
                    extraCredits += stack.getBaseValuePerUnit() * stack.getSize();
                }
            }
            if (recordShip) {
                if (playerFleet.getFleetData() != null)
                    for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                        extraCredits += member.getBaseValue();
                    }
            }
            textPanel.addPara(d_i18n.get("recordExtraCredits_success") + " %s ", Misc.getHighlightColor(), Misc.getDGSCredits(extraCredits));
            pregenInheritData.inheritCredits += extraCredits;
            record(selectedOption);
        }

        @Override
        public void customDialogCancel() {
            optionSelected(null, OptionID.CHECK_RECORD_SLOTS);
        }

        @Override
        public CustomUIPanelPlugin getCustomPanelPlugin() {
            return null;
        }
    }
}