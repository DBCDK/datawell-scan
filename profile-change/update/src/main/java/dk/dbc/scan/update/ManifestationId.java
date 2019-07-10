/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-profile-change-update
 *
 * datawell-scan-profile-change-update is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-profile-change-update is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.update;

import dk.dbc.search.solrdocstore.queue.QueueJob;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ManifestationId {

    private final int agencyId;
    private final String classifier;
    private final String bibliographicRecordId;

    public ManifestationId(String dashed) {
        String[] parts = dashed.split("[^0-9a-zA-Z]", 3);
        if (parts.length != 3 ||
            parts[0].length() == 0 ||
            parts[1].length() == 0 ||
            parts[2].length() == 0)
            throw new IllegalArgumentException("Argument: " + dashed + " is not a manifestationId from id field");
        this.agencyId = Integer.parseUnsignedInt(parts[0]);
        this.classifier = parts[1];
        this.bibliographicRecordId = parts[2];
    }

    public int getAgencyId() {
        return agencyId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getBibliographicRecordId() {
        return bibliographicRecordId;
    }

    public QueueJob asQueueJob() {
        return new QueueJob(agencyId, classifier, bibliographicRecordId);
    }

    @Override
    public String toString() {
        return "ManifestationId{" + agencyId + '-' + classifier + ':' + bibliographicRecordId + '}';
    }

}
