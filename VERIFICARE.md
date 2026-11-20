# Ce a fost verificat și ce nu

## Verificat contra codului sursă oficial Fabric API (branch 26.1.2, v0.155.3)
- Build: Loom `1.19-SNAPSHOT`, plugin `net.fabricmc.fabric-loom`, Loader 0.20.1, Gradle 9.8.2, Java 25, Fabric API `0.155.3+26.1.2` (copiate din `fabric-example-mod`).
- Rețea: `PayloadTypeRegistry.clientboundPlay().register`, `CustomPacketPayload.codec`, `ServerPlayNetworking.send/canSend`, `ClientPlayNetworking.registerGlobalReceiver`.
- HUD: `HudElementRegistry.attachElementAfter`, `VanillaHudElements.TITLE_AND_SUBTITLE`, `GuiGraphicsExtractor.fill/text/centeredText/pose().pushMatrix/scale/popMatrix`, `DeltaTracker`.
- Evenimente: `ServerPlayerEvents.JOIN/LEAVE/AFTER_RESPAWN`, `ServerLifecycleEvents.SERVER_STARTED/SERVER_STOPPING/BEFORE_SAVE`, `ServerTickEvents.END_SERVER_TICK`, `ClientPlayConnectionEvents.DISCONNECT`.
- Permisiuni: `PermissionPredicates.require(Identifier, PermissionLevel)`, `PermissionLevel.GAMEMASTERS`.
- Persistență: `AttachmentRegistry.create(id, b -> b.persistent(codec))`, `getAttached/setAttached`.
- Clase vanilla: căile de import și apelurile folosite direct de Fabric API (`MobEffectInstance(holder, ticks, amp)`, `addEffect`, `Player.drop(stack, bool, bool)`, `EntityType.create(level, EntitySpawnReason)`, `AttributeModifier(Identifier, double, Operation.ADD_VALUE)`, `MinecraftServer.runCommand(String)`, `sendSystemMessage`, `FoodData.getFoodLevel`).

## NEVERIFICAT (nu am avut surse vanilla 26.1; sunt scrise din cunoașterea API-ului 1.21.x)
Dacă `./gradlew build` dă erori, aproape sigur sunt aici:
1. `MobEffects.*` în afară de BLINDNESS/POISON/REGENERATION/ABSORPTION/NIGHT_VISION (SPEED, SLOWNESS, HASTE, MINING_FATIGUE, STRENGTH, JUMP_BOOST, RESISTANCE, DARKNESS, WEAKNESS, HUNGER, WITHER) și `ServerPlayer.removeEffect(Holder)`.
2. `Player.totalExperience` (câmp) și `giveExperiencePoints(int)` (Double XP).
3. `Attributes.JUMP_STRENGTH`, `AttributeInstance.addTransientModifier/removeModifier(Identifier)` (Gravity Trouble).
4. `FoodData` (pachet `net.minecraft.world.food`) și `setFoodLevel`.
5. `Entity.setRemainingFireTicks`, `ServerLevel.sendParticles(...)` cu 9 argumente, `ParticleTypes.FLAME/LAVA` (Firestorm).
6. `Level.noCollision(Entity)`, `Mob.setTarget`, `Mob.setItemSlot`, `EquipmentSlot.MAINHAND`, `EntitySpawnReason.EVENT`, `ItemEntity` (`net.minecraft.world.entity.item`), `BlockPos.containing`, `BlockState.isAir`.
7. Argumentul `/weather thunder 75s` (în 26.1 vremea s-a mutat în `WeatherData`; de aceea folosesc comanda, nu API-ul intern).
8. **Mixin Lost Items** (cel mai riscant): `ServerGamePacketListenerImpl#handleContainerClick`, `#handlePlayerAction`, câmpul `player`, `ServerboundPlayerActionPacket#getAction()` și `Action.DROP_ITEM/DROP_ALL_ITEMS/SWAP_ITEM_WITH_OFFHAND`, `AbstractContainerMenu#sendAllDataToRemote()`. Injectările au `require = 0` și configul mixin e `required: false`: dacă o țintă a fost redenumită, jocul pornește, dar evenimentul Lost Items nu blochează nimic. Verifică-l manual.
9. Permission API: modulul există în sursa 0.155.3, dar nu am putut confirma că jar-ul publicat îl include (Maven blocat). Dacă lipsește la compilare, înlocuiește în `EventCommands` cu `.requires(src -> src.hasPermission(...))` (spune-mi și o fac eu).

## Netestat (nimic nu a rulat)
Compilarea, cele 30 de evenimente, HUD-ul, singleplayer și multiplayer. Nu am avut JDK 25, nici acces la Maven Fabric/Mojang, nici client Minecraft. Singura verificare automată: `javac` (JDK 21, fără Minecraft pe classpath) nu a găsit erori de sintaxă, iar toate simbolurile nerezolvate sunt clase Minecraft/Fabric, niciuna din codul meu.

## Abateri de la cerință (și motivul)
- **Thunderstorm** e vreme globală: o văd toți jucătorii. Minecraft nu are vreme per jucător.
- **Peaceful Moment / Cursed Night** acționează pe monștrii din apropiere; dacă alt jucător e lângă tine, monștrii lui sunt afectați la fel.
- **Event History** se deschide cu `/event history` în chat (nu într-un ecran grafic), pentru că API-ul de ecrane din 26.1 nu a putut fi verificat.
- **Moarte:** evenimentele active se termină; cronometrul continuă.
- Țintă **26.1.2**, nu 26.1.0 (Fabric publică ultimul patch; 26.2 există, dar nu e vizat).
