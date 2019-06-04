/*
 * Copyright (C) 2019 DBC A/S (http://dbc.dk/)
 *
 * This is part of datawell-scan-service
 *
 * datawell-scan-service is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * datawell-scan-service is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.scan.service;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Generic class for checking a data structure with undetermined types
 * <p>
 * This implementation does not support null values as objects, they'll be
 * considered as if a check has failed.
 * <p>
 *
 * @param <T> type the the object to validate / extract from
 * @param <E> the exception type in case of failure
 */
public class Checker<T, E extends Exception> {

    public static class Builder<T> {

        private final T o;

        private Builder(T o) {
            this.o = o;
        }

        /**
         * Set the exception producer
         *
         * @param <Ex>  type of the exception
         * @param newEx function that given current value produces an exception
         * @return checker with new exception producer
         * @throws Ex if the initial value is null
         */
        public <Ex extends Exception> Checker<T, Ex> raises(Function<T, Ex> newEx) throws Ex {
            return new CheckerNotNullable<>(o, () -> newEx.apply(o));
        }
    }

    public static class BuilderNullable<T> {

        private final T o;

        private BuilderNullable(T o) {
            this.o = o;
        }

        /**
         * Set the exception producer
         *
         * @param <Ex>  type of the exception
         * @param newEx function that given current value produces an exception
         * @return checker with new exception producer
         * @throws Ex if the initial value is null
         */
        public <Ex extends Exception> Checker<T, Ex> raises(Function<T, Ex> newEx) throws Ex {
            return new Checker<>(o, () -> newEx.apply(o));
        }
    }

    /**
     * Create a checker that does not allow null values
     *
     * @param <T> type the the object to validate / extract from
     * @param o   the object in question
     * @return checker object with generic error message
     */
    public static <T> Builder<T> of(T o) {
        return new Builder<>(o);
    }

    /**
     * Create a checker that allows null values
     *
     * @param <T> type the the object to validate / extract from
     * @param o   the object in question
     * @return checker object with generic error message
     */
    public static <T> BuilderNullable<T> ofNullable(T o) {
        return new BuilderNullable<>(o);
    }

    protected final T o;
    protected final Supplier<E> ex;

    Checker(T o, Supplier<E> ex) throws E {
        this.o = o;
        this.ex = ex;
    }

    /**
     * New object with values
     * <p>
     * This is used by inherited classes to generate object of right type
     *
     * @param <R> type of new value
     * @param t   new value
     * @param ex  exception supplier
     * @return new checker
     * @throws E if an error occurs in setting new value (invalid value)
     */
    protected <R> Checker<R, E> withNewValue(R t, Supplier<E> ex) throws E {
        return new Checker<>(t, ex);
    }

    /**
     * Set the exception producer
     *
     * @param <Ex>  type of the exception
     * @param newEx function that given current value produces an exception
     * @return checker with new exception producer
     * @throws Ex This never happens, since
     */
    public <Ex extends Exception> Checker<T, Ex> raises(Function<T, Ex> newEx) throws Ex {
        return new Checker<>(o, () -> newEx.apply(o));
    }

    /**
     * Convert into a type, if possible
     *
     * @param <R> wanted type
     * @param t   type object
     * @return checker with same value but new type
     * @throws E if value not of wanted type
     */
    public <R> Checker<R, E> as(Class<R> t) throws E {
        if(o == null)
            return withNewValue((R) o, ex);
        if (t.isAssignableFrom(o.getClass()))
            return withNewValue((R) o, ex);
        throw ex.get();
    }

    /**
     * Convert an object
     *
     * @param <R>    return type
     * @param mapper converter function
     * @return new checker with the returned value
     * @throws E if the mapper returns null
     */
    <R> Checker<R, E> mapTo(Function<T, R> mapper) throws E {
        return withNewValue(mapper.apply(o), ex);
    }

    /**
     * Ensure a predicate is valid
     *
     * @param check predicate to validate
     * @return self
     * @throws E if the check fails
     */
    public Checker<T, E> ensure(Predicate<T> check) throws E {
        if (check.test(o))
            return this;
        throw ex.get();
    }

    /**
     * Extract the value or raise an exception
     *
     * @return the current values of the checker
     */
    public T get() {
        return o;
    }

}
