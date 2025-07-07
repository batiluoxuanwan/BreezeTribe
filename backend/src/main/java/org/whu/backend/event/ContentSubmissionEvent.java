package org.whu.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 内容提交事件
 * 当有任何新内容被创建时，就发布这个事件。它像一张信息卡片，包含了审核所需的所有信息。
 */
@Getter
public class ContentSubmissionEvent extends ApplicationEvent {
    private final String itemId;          // 内容的ID (游记ID或评论ID)
    private final ModerationItemType itemType; // 内容的类型
    private final String textContent;     // 需要被审核的文本内容

    public ContentSubmissionEvent(Object source, String itemId, ModerationItemType itemType, String textContent) {
        super(source);
        this.itemId = itemId;
        this.itemType = itemType;
        this.textContent = textContent;
    }

    /**
     * 【新增】需要AI审核的内容类型枚举
     */
    public enum ModerationItemType {
        TRAVEL_POST,
        POST_COMMENT,
        PACKAGE_COMMENT
    }
}