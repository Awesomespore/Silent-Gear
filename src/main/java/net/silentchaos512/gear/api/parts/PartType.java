/*
 * Silent Gear -- PartType
 * Copyright (C) 2018 SilentChaos512
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 3
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.silentchaos512.gear.api.parts;

import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class PartType {
    private static final Map<String, PartType> VALUES = new HashMap<>();

    public static final PartType BOWSTRING = create("bowstring", "B", PartBowstring::new);
    public static final PartType DUMMY = create("dummy", "d", ItemPart.Dummy::typeFactory);
    public static final PartType GRIP = create("grip", "G", PartGrip::new);
    public static final PartType MAIN = create("main", "M", PartMain::new);
    public static final PartType MISC_UPGRADE = create("misc_upgrade", "U", PartUpgrade::new);
    public static final PartType ROD = create("rod", "R", PartRod::new);
    public static final PartType TIP = create("tip", "T", PartTip::new);

    public static PartType create(String name, String debugSymbol, Function<PartOrigins, ItemPart> partConstructor) {
        if (VALUES.containsKey(name))
            throw new IllegalArgumentException(String.format("Already have PartType \"%s\"", name));

        PartType type = new PartType(name, debugSymbol, partConstructor);
        VALUES.put(name, type);
        return type;
    }

    @Nullable
    public static PartType get(String name) {
        return VALUES.get(name);
    }

    @Getter private final String name;
    @Getter private final String debugSymbol;
    private final Function<PartOrigins, ItemPart> partConstructor;

    private PartType(String name, String debugSymbol, Function<PartOrigins, ItemPart> partConstructor) {
        this.name = name;
        this.debugSymbol = debugSymbol;
        this.partConstructor = partConstructor;
    }

    public ItemPart construct(PartOrigins origin) {
        return this.partConstructor.apply(origin);
    }
}
