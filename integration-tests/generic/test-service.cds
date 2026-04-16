using test.data.model as db from '../db/data-model';

annotate db.Roots.sizeLimitedAttachments with {
    content @Validation.Maximum: '5MB';
};

// Media type validation for attachments - for testing purposes.
annotate db.Roots.mediaValidatedAttachments with {
    content @(Core.AcceptableMediaTypes: [
        'image/jpeg',
        'image/png'
    ]);
}

annotate db.Roots.mimeValidatedAttachments with {
    content @(Core.AcceptableMediaTypes: ['application/pdf']);
}

service TestService {
    entity Roots            as projection on db.Roots;
    entity AttachmentEntity as projection on db.AttachmentEntity;
    entity InlineOnlyTable  as projection on db.InlineOnly;
}

service TestDraftService {
    @odata.draft.enabled
    entity DraftRoots as projection on db.Roots;
    @odata.draft.enabled
    entity DraftInlineOnly as projection on db.InlineOnly;
}
