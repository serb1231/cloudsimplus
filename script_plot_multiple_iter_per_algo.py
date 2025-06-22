import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path
import numpy as np

# Define directories
input_dir = Path("results_csv/results_multiple_iterations_algorithm")
output_dir = Path("plots/plots_algorithms_ran_multiple_times")
output_dir.mkdir(parents=True, exist_ok=True)

# Get all CSV files from input directory
csv_files = list(input_dir.glob("*.csv"))

# Plot for each CSV file
for csv_file in csv_files:
    df = pd.read_csv(csv_file)
    algorithm_name = csv_file.stem

    # Normalize energy saved relative to max power in the file
    max_power = df["PowerConsumption"].max()
    df["EnergySaved"] = (max_power - df["PowerConsumption"]) / max_power * 100
    df["SLA_Success"] = 100 - df["ViolationPct"]

    # Remove outliers in SLA_Success using IQR method
    Q1 = df["SLA_Success"].quantile(0.25)
    Q3 = df["SLA_Success"].quantile(0.75)
    IQR = Q3 - Q1
    lower_bound = Q1 - 1.5 * IQR
    upper_bound = Q3 + 1.5 * IQR
    df = df[(df["SLA_Success"] >= lower_bound) & (df["SLA_Success"] <= upper_bound)]

    # Create plot
    plt.figure(figsize=(10, 6))

    # Plot all individual runs in gray
    for _, group in df.groupby("Algorithm"):
        plt.plot(group["EnergySaved"], group["SLA_Success"], color="gray", alpha=0.4)

    # Bin energy saved into 10 intervals
    bins = np.linspace(0, 100, 11)
    df["EnergyBin"] = pd.cut(df["EnergySaved"], bins, include_lowest=True)

    # Compute average SLA success and average energy saved per bin
    bin_means = df.groupby("EnergyBin").agg({
        "EnergySaved": "mean",
        "SLA_Success": "mean"
    }).dropna()

    # Plot average line
    plt.plot(bin_means["EnergySaved"], bin_means["SLA_Success"],
             color="black", linewidth=2.5, marker='o', label="Average")

    # Labeling
    plt.xlabel("Energy Saved (%)")
    plt.ylabel("SLA Success (%)")
    plt.title(f"SLA Success vs Energy Saved - {algorithm_name}")
    plt.grid(True)
    plt.legend()
    plt.tight_layout()

    # Save plot
    output_path = output_dir / f"{algorithm_name}_evolution_energy_saved_no_outliers.png"
    plt.savefig(output_path)
    plt.close()

print("✅ Plots saved to:", output_dir)
