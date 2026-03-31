using { mt.test.data as db } from '../db/index';

service MtTestService {
    entity Documents as projection on db.Documents;
}
