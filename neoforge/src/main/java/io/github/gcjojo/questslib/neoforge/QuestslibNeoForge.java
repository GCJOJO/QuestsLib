package io.github.gcjojo.questslib.neoforge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import io.github.gcjojo.questslib.QuestsLib;
import io.github.gcjojo.questslib.neoforge.commands.NeoForgeCommandRegister;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;


@Mod(QuestsLib.MOD_ID)
public final class QuestslibNeoForge {

    public QuestslibNeoForge() {
        // Run our common setup.
        QuestsLib.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> QuestsLib::initClient);
        NeoForge.EVENT_BUS.register(new NeoForgeCommandRegister());
    }
}
