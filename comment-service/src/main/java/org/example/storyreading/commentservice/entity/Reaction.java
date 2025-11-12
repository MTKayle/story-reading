package org.example.storyreading.commentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long commentId;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private ReactionType type; // LIKE, DISLIKE, REPORT

    public enum ReactionType {
        LIKE,
        TYM,
        HAHA,
        SAD,
        ANGRY,
        WOW
    }
}

