/*
 * Silent Gear -- ToolRods
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

package net.silentchaos512.gear.item;

import lombok.Getter;
import net.silentchaos512.gear.api.parts.PartRod;

import java.util.Locale;

public enum ToolRods {
    WOOD, BONE, STONE, IRON, BLAZE, END;

    @Getter
    private final PartRod part;

    ToolRods() {
        this.part = new PartRod();
    }

    public String getPartName() {
        return "rod_" + this.name().toLowerCase(Locale.ROOT);
    }
}
