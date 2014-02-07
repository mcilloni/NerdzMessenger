/*
 * This file is part of NerdzApi-java.
 *
 *     NerdzApi-java is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     NerdzApi-java is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with NerdzApi-java.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     (C) 2013 Marco Cilloni <marco.cilloni@yahoo.com>
 */

package eu.nerdz.app;

/**
 * Simple Constants.
 */
public class Keys {

    //Just, don't instantiate this, please
    private Keys() {
        throw new AssertionError();
    }

    public static final int MESSAGE = 1;
    public static final String FROM = "from";
    public static final String FROM_ID = "fromId";
    public static final String NERDZ_INFO = "NerdzInfo";
    public static final String OPERATION_RESULT = "ResultItemZKS";
}
