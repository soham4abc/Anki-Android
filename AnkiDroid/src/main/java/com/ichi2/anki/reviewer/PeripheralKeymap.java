/*
 Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>                     
                                                                                     
 This program is free software; you can redistribute it and/or modify it under       
 the terms of the GNU General Public License as published by the Free Software       
 Foundation; either version 3 of the License, or (at your option) any later          
 version.                                                                            
                                                                                     
 This program is distributed in the hope that it will be useful, but WITHOUT ANY     
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A     
 PARTICULAR PURPOSE. See the GNU General Public License for more details.            
                                                                                     
 You should have received a copy of the GNU General Public License along with        
 this program.  If not, see <http://www.gnu.org/licenses/>.                          
 */

package com.ichi2.anki.reviewer;

import android.view.KeyEvent;

import com.ichi2.anki.cardviewer.ViewerCommand;
import com.ichi2.anki.cardviewer.ViewerCommand.CommandProcessor;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Accepts peripheral input, mapping via various keybinding strategies,
 * and converting them to commands for the Reviewer. */
public class PeripheralKeymap {

    private final KeyMap mKeyMap;

    private boolean mHasSetup = false;

    public PeripheralKeymap(ReviewerUi reviewerUi, CommandProcessor commandProcessor) {
        this.mKeyMap = new KeyMap(commandProcessor, reviewerUi);
    }

    public void setup() {
        for (PeripheralCommand command : PeripheralCommand.getDefaultCommands()) {
            mKeyMap.addCommand(command, command.getSide());
        }

        mHasSetup = true;
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!mHasSetup || event.getRepeatCount() > 0) {
            return false;
        }

        return mKeyMap.onKeyUp(keyCode, event);
    }

    @SuppressWarnings( {"unused", "RedundantSuppression"})
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public static class KeyMap {
        public final HashMap<MappableBinding, ViewerCommand> mBindingMap = new HashMap<>();
        private final CommandProcessor mProcessor;
        private final ReviewerUi mReviewerUI;


        public KeyMap(CommandProcessor commandProcessor, ReviewerUi mReviewerUi) {
            this.mProcessor = commandProcessor;
            this.mReviewerUI = mReviewerUi;
        }

        @SuppressWarnings( {"unused", "RedundantSuppression"})
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            boolean ret = false;

            List<Binding> bindings = Binding.key(event);
            CardSide side = CardSide.fromAnswer(mReviewerUI.isDisplayingAnswer());

            for (Binding b: bindings) {

                MappableBinding binding = new MappableBinding(b, side);
                ViewerCommand command = mBindingMap.get(binding);
                if (command == null) {
                    continue;
                }

                ret |= mProcessor.executeCommand(command);
            }

            return ret;
        }


        public void addCommand(@NonNull PeripheralCommand command, CardSide side) {
            MappableBinding key = new MappableBinding(command.getBinding(), side);
            set(key, command.getCommand());
        }


        public void set(@NonNull MappableBinding key, @NonNull ViewerCommand value) {
            mBindingMap.put(key, value);
        }


        @Nullable
        public ViewerCommand get(MappableBinding key) {
            return mBindingMap.get(key);
        }
    }
}
