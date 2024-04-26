package com.example.mumuchat;

public class ChatBody {
    private String id;
    private String object;
    private int created;
    private String result;
    private boolean is_truncated;
    private boolean need_clear_history;
    private Object usage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public int getCreated() {
        return created;
    }

    public void setCreated(int created) {
        this.created = created;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isIs_truncated() {
        return is_truncated;
    }

    public void setIs_truncated(boolean is_truncated) {
        this.is_truncated = is_truncated;
    }

    public boolean isNeed_clear_history() {
        return need_clear_history;
    }

    public void setNeed_clear_history(boolean need_clear_history) {
        this.need_clear_history = need_clear_history;
    }

    public Object getUsage() {
        return usage;
    }

    public void setUsage(Object usage) {
        this.usage = usage;
    }
}
