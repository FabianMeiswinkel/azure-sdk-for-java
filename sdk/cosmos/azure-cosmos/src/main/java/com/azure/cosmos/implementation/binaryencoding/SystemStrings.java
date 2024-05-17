// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import static com.azure.cosmos.implementation.guava25.base.Preconditions.checkNotNull;

public final class SystemStrings
{
    /// <summary>
    /// List of system strings
    /// </summary>
    public static final UtfAllString[] Strings = new UtfAllString[]
    {
        UtfAllString.create("$s"),
        UtfAllString.create("$t"),
        UtfAllString.create("$v"),
        UtfAllString.create("_attachments"),
        UtfAllString.create("_etag"),
        UtfAllString.create("_rid"),
        UtfAllString.create("_self"),
        UtfAllString.create("_ts"),
        UtfAllString.create("attachments/"),
        UtfAllString.create("coordinates"),
        UtfAllString.create("geometry"),
        UtfAllString.create("GeometryCollection"),
        UtfAllString.create("id"),
        UtfAllString.create("url"),
        UtfAllString.create("Value"),
        UtfAllString.create("label"),
        UtfAllString.create("LineString"),
        UtfAllString.create("link"),
        UtfAllString.create("MultiLineString"),
        UtfAllString.create("MultiPoint"),
        UtfAllString.create("MultiPolygon"),
        UtfAllString.create("name"),
        UtfAllString.create("Name"),
        UtfAllString.create("Type"),
        UtfAllString.create("Point"),
        UtfAllString.create("Polygon"),
        UtfAllString.create("properties"),
        UtfAllString.create("type"),
        UtfAllString.create("value"),
        UtfAllString.create("Feature"),
        UtfAllString.create("FeatureCollection"),
        UtfAllString.create("_id"),
    };

    public static Integer GetSystemStringId(Utf8ByteBuffer buffer) {
        checkNotNull(buffer, "Parameter 'buffer' MUST NOT be null.");
        switch (buffer.getLength()) {
            case 2: return getSystemStringIdLength2(buffer);
            case 3: return getSystemStringIdLength3(buffer);
            case 4: return getSystemStringIdLength4(buffer);
            case 5: return getSystemStringIdLength5(buffer);
            case 7: return getSystemStringIdLength7(buffer);
            case 8: return getSystemStringIdLength8(buffer);
            case 10: return getSystemStringIdLength10(buffer);
            case 11: return getSystemStringIdLength11(buffer);
            case 12: return getSystemStringIdLength12(buffer);
            case 15: return getSystemStringIdLength15(buffer);
            case 17: return getSystemStringIdLength17(buffer);
            case 18: return getSystemStringIdLength18(buffer);
            default: return null;
        }
    }

    private static Integer getSystemStringIdLength2(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[0]))
        {
            return 0;
        }

        if (buffer.equals(Strings[1]))
        {
            return 1;
        }

        if (buffer.equals(Strings[2]))
        {
            return 2;
        }

        if (buffer.equals(Strings[12]))
        {
            return 12;
        }

        return null;
    }

    private static Integer getSystemStringIdLength3(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[7]))
        {
            return 7;
        }

        if (buffer.equals(Strings[13]))
        {
            return 13;
        }

        if (buffer.equals(Strings[31]))
        {
            return 31;
        }

        return null;
    }

    private static Integer getSystemStringIdLength4(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[5]))
        {
            return 5;
        }

        if (buffer.equals(Strings[17]))
        {
            return 17;
        }

        if (buffer.equals(Strings[21]))
        {
            return 21;
        }

        if (buffer.equals(Strings[22]))
        {
            return 22;
        }

        if (buffer.equals(Strings[23]))
        {
            return 23;
        }

        if (buffer.equals(Strings[27]))
        {
            return 27;
        }

        return null;
    }

    private static Integer getSystemStringIdLength5(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[4]))
        {
            return 4;
        }

        if (buffer.equals(Strings[6]))
        {
            return 6;
        }

        if (buffer.equals(Strings[14]))
        {
            return 14;
        }

        if (buffer.equals(Strings[15]))
        {
            return 15;
        }

        if (buffer.equals(Strings[24]))
        {
            return 24;
        }

        if (buffer.equals(Strings[28]))
        {
            return 28;
        }

        return null;
    }

    private static Integer getSystemStringIdLength7(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[25]))
        {
            return 25;
        }

        if (buffer.equals(Strings[29]))
        {
            return 29;
        }

        return null;
    }

    private static Integer getSystemStringIdLength8(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[10]))
        {
            return 10;
        }

        return null;
    }

    private static Integer getSystemStringIdLength10(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[16]))
        {
            return 16;
        }

        if (buffer.equals(Strings[19]))
        {
            return 19;
        }

        if (buffer.equals(Strings[26]))
        {
            return 26;
        }

        return null;
    }

    private static Integer getSystemStringIdLength11(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[9]))
        {
            return 9;
        }

        return null;
    }

    private static Integer getSystemStringIdLength12(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[3]))
        {
            return 3;
        }

        if (buffer.equals(Strings[8]))
        {
            return 8;
        }

        if (buffer.equals(Strings[20]))
        {
            return 20;
        }

        return null;
    }

    private static Integer getSystemStringIdLength15(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[18]))
        {
            return 18;
        }

        return null;
    }

    private static Integer getSystemStringIdLength17(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[30]))
        {
            return 30;
        }

        return null;
    }

    private static Integer getSystemStringIdLength18(Utf8ByteBuffer buffer)
    {
        if (buffer.equals(Strings[11]))
        {
            return 11;
        }

        return null;
    }

    /// <summary>
    /// Gets the SystemStringId for a particular system string.
    /// </summary>
    /// <param name="utf8Span">The system string to get the enum id for.</param>
    /// <param name="systemStringId">The id of the system string if found.</param>
    /// <returns>The SystemStringId for a particular system string.</returns>
    public static TryResult<Integer> tryGetSystemStringId(Utf8ByteBuffer utf8buffer)
    {
        Integer id = SystemStrings.GetSystemStringId(utf8buffer);
        if (id == null)
        {
            return TryResult.failed(Integer.class);
        }

        return TryResult.success(id);
    }

    public static TryResult<UtfAllString> tryGetSystemStringById(int id)
    {
        if (id >= SystemStrings.Strings.length)
        {
            TryResult.failed(UtfAllString.class);
        }

        return TryResult.success(SystemStrings.Strings[id]);
    }
}
