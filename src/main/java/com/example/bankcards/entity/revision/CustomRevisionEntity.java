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
public class CustomRevisionEntity {

    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CardAuditRevisionSeq")
    @SequenceGenerator(name = "CardAuditRevisionSeq", sequenceName = "card_audit_revision_seq", allocationSize = 1)
    private long id;

    @RevisionTimestamp
    @Column(name = "timestamp")
    private long timestamp;

    @Column(name = "username")
    private String username;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomRevisionEntity that = (CustomRevisionEntity) o;
        return id == that.id &&
                timestamp == that.timestamp &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, username);
    }

}
