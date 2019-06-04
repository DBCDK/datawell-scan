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
import java.util.function.Supplier;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
class CheckerNotNullable<T, E extends Exception> extends Checker<T, E> {

    CheckerNotNullable(T o, Supplier<E> ex) throws E {
        super(o, ex);
        if (o == null)
            throw ex.get();
    }

    @Override
    protected <R> Checker<R, E> withNewValue(R t, Supplier<E> ex) throws E {
        return new CheckerNotNullable<>(t, ex);
    }

    @Override
    public <Ex extends Exception> Checker<T, Ex> raises(Function<T, Ex> newEx) throws Ex {
        return new CheckerNotNullable<>(o, () -> newEx.apply(o));
    }

}
