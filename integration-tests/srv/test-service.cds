using test.data.model as db from '../db/data-model';

annotate db.Roots.sizeLimitedAttachments with {
    content @Validation.Maximum: '5MB';
};

service TestService {
    entity Roots            as projection on db.Roots;
    entity AttachmentEntity as projection on db.AttachmentEntity;
}

service TestDraftService {
    @odata.draft.enabled
    entity DraftRoots as projection on db.Roots;
}
