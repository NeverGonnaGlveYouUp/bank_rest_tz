package com.example.bankcards.service.revisionListener;

import com.example.bankcards.entity.revision.CardRevision;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class CardRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CardRevision revision = (CardRevision) revisionEntity;

        revision.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
