/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of scan-service
 *
 * scan-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * scan-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.service;

import java.util.List;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class ScanResponse {

    public static class Success {

        private Request request;
        private Result result;

        public Success() {
        }

        public Success(Request request, Result result) {
            this.request = request;
            this.result = result;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public Result getResult() {
            return result;
        }

        public void setResult(Result result) {
            this.result = result;
        }

    }

    public static class Failure {

        private String error;
        private Request request;

        public Failure() {
        }

        public Failure(String error, Request request) {
            this.error = error;
            this.request = request;
        }

        public String getError() {
            return error;
        }

        public void setError(String errorMessage) {
            this.error = errorMessage;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }
    }

    public static class Request {

        private Integer agencyId;
        private String profile;
        private String term;
        private String register;
        private Integer count;
        private Boolean cont;
        private String trackingId;

        public Request() {
        }

        public Request(Integer agencyId, String profile, String term, String register, Integer count, Boolean cont, String trackingId) {
            this.agencyId = agencyId;
            this.profile = profile;
            this.term = term;
            this.register = register;
            this.count = count;
            this.cont = cont;
            this.trackingId = trackingId;
        }

        public Integer getAgencyId() {
            return agencyId;
        }

        public void setAgencyId(Integer agencyId) {
            this.agencyId = agencyId;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public String getRegister() {
            return register;
        }

        public void setRegister(String register) {
            this.register = register;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Boolean getContinue() {
            return cont;
        }

        public void setContinue(Boolean cont) {
            this.cont = cont;
        }

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

        @Override
        public String toString() {
            return "RequestParam{" + "agencyId=" + agencyId + ", profile=" + profile + ", term=" + term + ", register=" + register + ", count=" + count + ", continue=" + cont + ", trackingId=" + trackingId + '}';
        }
    }

    public static class Result {

        private String continueAfter;
        private List<Term> terms;

        public Result() {
        }

        public Result(String continueAfter, List<Term> terms) {
            this.continueAfter = continueAfter;
            this.terms = terms;
        }

        public String getContinueAfter() {
            return continueAfter;
        }

        public void setContinueAfter(String continueAfter) {
            this.continueAfter = continueAfter;
        }

        public List<Term> getTerms() {
            return terms;
        }

        public void setTerms(List<Term> terms) {
            this.terms = terms;
        }
    }

    public static class Term {

        private String term;
        private long count;

        public Term() {
        }

        public Term(String term) {
            this.term = term;
            this.count = -1;
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public boolean hasTerms() {
            return count > 0;
        }

        public boolean notVerified() {
            return count == -1;
        }

        @Override
        public String toString() {
            return "Term{" + "term=" + term + ", count=" + count + '}';
        }
    }
}
