package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRules;

public class UNGP_BirthdayPresent extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return "1";
        return null;
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            int[] giftTime = (int[]) Global.getSector().getMemoryWithoutUpdate().get("$" + rule.getBuffID());
            if (giftTime != null) {
                CampaignClockAPI clock = Global.getSector().getClock();
                if (clock.getCycle() == giftTime[0] && clock.getMonth() == giftTime[1] && clock.getDay() == giftTime[2]) {
                    UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
                    List<URule> activatedRules = inGameData.getActivatedRules();
                    List<URule> allRules = getAllRules();
                    WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(Misc.getRandom(clock.getCycle() + clock.getMonth() + clock.getDay(), inGameData.getCurCycle()));
                    for (URule uRule : allRules) {
                        if (!uRule.isBonus() || activatedRules.contains(uRule)) continue;
                        if (!uRule.isRollable()) continue;
                        picker.add(uRule, Math.abs(uRule.getCost()));
                    }
                    URule toAdd = picker.pick();
                    activatedRules.remove(rule);
                    if (toAdd != null) {
                        activatedRules.add(toAdd);
                        MessageIntel intel = new MessageIntel(rule.getName(), toAdd.getCorrectColor());
                        intel.setIcon(toAdd.getSpritePath());
                        intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{toAdd.getName()}, toAdd.getCorrectColor());
                        showMessage(intel);
                        Global.getSoundPlayer().playUISound("UNGP_birthdaypresent_activate", 1f, 1f);
                    }
                    inGameData.saveActivatedRules(activatedRules);
                    UNGP_RulesManager.updateCacheNextFrame();
                }
            }
        }
    }

    @Override
    public void applyGlobalStats() {
        if (!Global.getSector().getMemoryWithoutUpdate().contains("$" + rule.getBuffID())) {
            CampaignClockAPI clock = Global.getSector().getClock();
            int[] giftTime = new int[]{clock.getCycle() + 1, clock.getMonth(), clock.getDay()};
            Global.getSector().getMemoryWithoutUpdate().set("$" + rule.getBuffID(), giftTime);
        }
    }

    @Override
    public void unapplyGlobalStats() {
        UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
        List<URule> activatedRules = inGameData.getActivatedRules();
        if (!activatedRules.contains(rule)) {
            Global.getSector().getMemoryWithoutUpdate().unset("$" + rule.getBuffID());
        }
    }
}