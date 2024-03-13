package com.space365.utility.recycle;

import android.util.Pair;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.util.List;

public class ItemTemplate {
    private final int variableId;
    @LayoutRes
    private final int templateId;

    private List<Pair<Integer,Object>> extraVariable;

    @NonNull
    public static ItemTemplate of(@LayoutRes int templateId, int variableId) {
        return new ItemTemplate(variableId, templateId);
    }

    public ItemTemplate(int variableId, @LayoutRes int templateId) {
        this.variableId = variableId;
        this.templateId = templateId;
    }

    public void setExtraVariable(List<Pair<Integer,Object>> extraVariable){
        this.extraVariable = extraVariable;
    }

    public List<Pair<Integer, Object>> getExtraVariable() {
        return extraVariable;
    }

    public int getTemplateId() {
        return templateId;
    }

    public int getVariableId() {
        return variableId;
    }
}
