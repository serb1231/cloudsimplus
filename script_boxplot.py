import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

# Define input and output directories
input_dir = Path("results_csv/results_multiple_iterations_algorithm_smaller_aco")
output_dir = Path("plots/boxplots")
output_dir.mkdir(parents=True, exist_ok=True)

# Store best results
all_data = []

# Process each CSV file
for file in input_dir.glob("*.csv"):
    df = pd.read_csv(file)

    # Extract run ID and algorithm base name
    df["RunId"] = df["Algorithm"].str.extract(r"times_(\d+)")
    df["BaseName"] = df["Algorithm"].str.extract(r"(^[^_]+)")

    if df.empty or df["RunId"].isnull().all():
        print(f"⚠ Skipping file {file.name} due to formatting issues.")
        continue

    # For each run ID, get the row with the lowest power where SLA violation < 10%
    for run_id, group in df.groupby("RunId"):
        valid = group[group["ViolationPct"] < 50]
        if not valid.empty:
            best_row = valid.loc[valid["PowerConsumption"].idxmin()]
            all_data.append({
                "Algorithm": best_row["BaseName"],
                "PowerConsumption": best_row["PowerConsumption"]
            })

# Create summary DataFrame
summary_df = pd.DataFrame(all_data)

# Boxplot
plt.figure(figsize=(10, 6))
summary_df.boxplot(by="Algorithm", column="PowerConsumption", grid=True)

# Labeling
plt.title("SLA-Compliant Power Consumption (<10% SLA Violations)")
plt.suptitle("")
plt.xlabel("Algorithm")
plt.ylabel("Power Consumption (W)")
plt.tight_layout()

# Save
plot_path = output_dir / "boxplot_best_power_sla_compliant_50_perc_smaller_aco.png"
plt.savefig(plot_path)
plt.close()

print(f"✅ Boxplot saved to: {plot_path}")


# okay, now help me think. So currently I have 2 boxplot graphs, comparing the algorithms, first only the varianta that have less than 10% sla violation, and then the variants that have less than 50% violations. currently I am comparing data for running all variations of combinations of hyperparameters for ACOalgorithm. what other graphs do you suggest that I do?