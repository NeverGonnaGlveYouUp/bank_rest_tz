package com.example.bankcards.entity.revision;

import com.example.bankcards.service.revisionListener.CardRevisionListener;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@RevisionEntity(CardRevisionListener.class)
@Table(name = "REVINFO")
public class CardRevision {

    @Id
    @RevisionNumber
    @GeneratedValue(generator = "CardAuditRevisionSeq")
    @SequenceGenerator(name = "CardAuditRevisionSeq", sequenceName = "card_audit_revision_seq", allocationSize = 1)
    private int id;

    @RevisionTimestamp
    private long timestamp;

    private String username;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CardRevision revision = (CardRevision) o;
        return getId() == revision.getId() && getTimestamp() == revision.getTimestamp() && Objects.equals(getUsername(), revision.getUsername());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTimestamp(), getUsername());
    }
}
