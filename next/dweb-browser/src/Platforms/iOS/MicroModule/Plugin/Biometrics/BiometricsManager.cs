﻿using LocalAuthentication;

namespace DwebBrowser.Platforms.iOS.MicroModule.Plugin.Biometrics;

public static class BiometricsManager
{
	static readonly Debugger Console = new("BiometricsManager");

	private static LAContext _context = new LAContext();

	public static bool Check()
	{
		var _bool = _context.CanEvaluatePolicy(LAPolicy.DeviceOwnerAuthenticationWithBiometrics, out var error);
		
		if (error is not null)
		{
			Console.Log("check", error.LocalizedDescription);
		}

		return _bool;
	}

	public static async Task<BiometricsResult> BiometricsAsync()
	{
		var (_bool, error) = await _context.EvaluatePolicyAsync(
			LAPolicy.DeviceOwnerAuthenticationWithBiometrics,
			"Access requires authentication");

		return new BiometricsResult(_bool, error?.LocalizedDescription ?? "");
    }

	public record BiometricsResult(bool success, string message);
}

