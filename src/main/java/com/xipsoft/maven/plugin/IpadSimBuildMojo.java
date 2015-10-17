package com.xipsoft.maven.plugin;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.robovm.compiler.AppCompiler;
import org.robovm.compiler.config.Arch;
import org.robovm.compiler.config.Config;
import org.robovm.compiler.config.OS;
import org.robovm.compiler.target.ios.DeviceType;
import org.robovm.compiler.target.ios.IOSSimulatorLaunchParameters;
import org.robovm.compiler.target.ios.IOSTarget;
import org.robovm.maven.plugin.AbstractIOSSimulatorMojo;

import java.io.File;
import java.io.IOException;

/**
 * Created by yinka on 10/16/15.
 */
@Mojo(name="ipad-sim-build", defaultPhase= LifecyclePhase.PACKAGE,
        requiresDependencyResolution= ResolutionScope.COMPILE_PLUS_RUNTIME)
public class IpadSimBuildMojo extends AbstractIOSSimulatorMojo {

    public static final DeviceType.DeviceFamily DEVICE_FAMILY = DeviceType.DeviceFamily.iPad;

    public IpadSimBuildMojo() {
        super(DEVICE_FAMILY);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Arch arch = Arch.x86_64;
            if (super.arch != null && super.arch.equals(Arch.x86.toString())) {
                arch = Arch.x86;
            }

            build(OS.ios, arch, IOSTarget.TYPE);
        } catch (Throwable t) {
            throw new MojoExecutionException("Failed to launch IOS Simulator", t);
        }
    }


    protected AppCompiler build(OS os, Arch arch, String targetType)
            throws MojoExecutionException, MojoFailureException {

        getLog().info("Building RoboVM app for: " + os + " (" + arch + ")");

        Config.Builder builder;
        try {
            builder = new Config.Builder();
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        configure(builder).os(os).arch(arch).targetType(targetType);
        String targetPath = System.getProperty("target-path");
        getLog().info("target-path = " + targetPath);
        if (targetPath != null && !targetPath.isEmpty()) {
            File targetFile = new File(targetPath);
            try {
                FileUtils.deleteDirectory(targetFile);
            } catch (IOException e) {
                throw new MojoExecutionException(
                        "Failed to clean output dir " + targetFile, e);
            }
            targetFile.mkdirs();
            builder.tmpDir(targetFile);
        }
//         execute the RoboVM build

        try {

            getLog().info(
                    "Compiling RoboVM app, this could take a while, especially the first time round");
            AppCompiler compiler = new AppCompiler(builder.build());
            compiler.build();
            Config config = compiler.getConfig();
            IOSSimulatorLaunchParameters launchParameters = (IOSSimulatorLaunchParameters)
                    config.getTarget().createLaunchParameters();

            // select the device based on the (optional) SDK version and (optional) device type
            DeviceType deviceType = DeviceType.getBestDeviceType(
                    arch, DEVICE_FAMILY, deviceName, sdk);
            launchParameters.setDeviceType(deviceType);
            compiler.launchAsync(launchParameters);
            return compiler;

        } catch (IOException e) {
            throw new MojoExecutionException(
                    "Error building RoboVM executable for app", e);
        } catch (Throwable e) {
            e.printStackTrace();
            throw new MojoExecutionException(
                    "Error building RoboVM executable for app", e);
        }
    }
}
