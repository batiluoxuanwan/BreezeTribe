package org.whu.backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.whu.backend.entity.Tag;

import java.util.Set;

/**
 * 定义一个通用的用户行为事件
 */
@Getter
public class UserInteractionEvent extends ApplicationEvent {
    private final String userId;
    private final Set<Tag> tags;
    private final int weight;

    public UserInteractionEvent(Object source, String userId, Set<Tag> tags, int weight) {
        super(source);
        this.userId = userId;
        this.tags = tags;
        this.weight = weight;
    }
}