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

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class CheckerTest {

    @Test(timeout = 2_000L)
    public void testSimpleSuccess() throws Exception {
        System.out.println("testSimpleSuccess");
        Object obj = new HashMap<String, Object>() {
            {
                put("a", "1");
                put("blah", new ArrayList<Object>() {
                {
                    add(42L);
                    add("what?");
                    add(false);
                }
            });
            }
        };

        Long answer = Checker.ofNullable(obj)
                .raises(o -> new Exception("error in: " + o))
                .as(Map.class)
                .ensure(o -> !o.isEmpty())
                .mapTo(o -> o.get("blah"))
                .as(List.class)
                .ensure(o -> o.size() > 1)
                .mapTo(o -> o.get(0))
                .as(Long.class)
                .get();

        assertThat(answer, is(42L));
    }

    @Test(timeout = 2_000L)
    public void testNullObject() throws Exception {
        System.out.println("testNullObject");
        Object ret = Checker.ofNullable(null)
                .raises(o -> new Exception("error in: " + o))
                .get();
        assertThat(ret, nullValue());
    }

    @Test(timeout = 2_000L, expected = Exception.class)
    public void testTypeError() throws Exception {
        System.out.println("testTypeError");
        Object obj = new HashMap<String, Object>();
        Checker.ofNullable(obj)
                .raises(o -> new Exception("error in: " + o))
                .as(List.class);
    }

    @Test(timeout = 2_000L, expected = Exception.class)
    public void testPredicateError() throws Exception {
        System.out.println("testPredicateError");
        HashMap<String, Object> obj = new HashMap<>();
        Checker.ofNullable(obj)
                .raises(o -> new Exception("error in: " + o))
                .ensure(o -> o.size() >= 1);
    }

    @Test(timeout = 2_000L, expected = RuntimeException.class)
    public void testChangeRaises() throws Exception {
        System.out.println("testChangeRaises");
        HashMap<String, Object> obj = new HashMap<>();
        Checker.ofNullable(obj)
                .raises(o -> new Exception("error in: " + o))
                .raises(o -> new RuntimeException("error in: " + o))
                .as(List.class);
    }

    @Test(timeout = 2_000L)
    public void testMapToNull() throws Exception {
        System.out.println("testMapToNull");
        HashMap<String, Object> obj = new HashMap<>();
        Object ret = Checker.ofNullable(obj)
                .raises(o -> new Exception("error in: " + o))
                .mapTo(o -> null)
                .get();
        assertThat(ret, nullValue());
    }

    @Test(timeout = 2_000L)
    public void testTypeOfNull() throws Exception {
        System.out.println("testTypeOfNull");
        Object ret = Checker.ofNullable(null)
                .raises(o -> new Exception("error in: " + o))
                .as(List.class)
                .get();
        assertThat(ret, nullValue());
    }
}
