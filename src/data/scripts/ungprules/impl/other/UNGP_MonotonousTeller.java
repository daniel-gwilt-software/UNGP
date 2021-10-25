package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

public class UNGP_MonotonousTeller extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    private int daysToLostStoryPoint;

    @Override
    public void updateDifficultyCache(int difficulty) {
        daysToLostStoryPoint = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return Math.round(getLinearValue(120f, 60f, difficulty));
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            int sinceLastLost;
            final String buffID = rule.getBuffID();
            if (getDataInCampaign(buffID) != null) {
                sinceLastLost = getDataInCampaign(buffID);
            } else {
                sinceLastLost = 0;
            }

            sinceLastLost++;
            if (sinceLastLost > daysToLostStoryPoint) {
                PersonAPI player = Global.getSector().getPlayerPerson();
                int storyPoints = player.getStats().getStoryPoints();
                if (storyPoints > 0) {
                    player.getStats().setStoryPoints(storyPoints - 1);
                    sinceLastLost = 0;

                    UNGP_SpecialistIntel.RuleMessage message = new UNGP_SpecialistIntel.RuleMessage(rule, rule.getExtra1(), "1");
                    message.send();
                }
            }
            saveDataInCampaign(buffID, sinceLastLost);
        }
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return "1";
        return null;
    }
}